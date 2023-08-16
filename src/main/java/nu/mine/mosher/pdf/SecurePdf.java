package nu.mine.mosher.pdf;

import com.itextpdf.io.font.constants.StandardFontFamilies;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.actions.data.ITextCoreProductData;
import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.xmp.*;
import com.itextpdf.kernel.xmp.options.IteratorOptions;
import com.itextpdf.kernel.xmp.properties.*;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.signatures.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nu.mine.mosher.gnopt.Gnopt;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalIsPresent"})
@Slf4j
public class SecurePdf {
    private static final float SCALE = 72.0f / 300.0f;
    private static final String NS_PUB = "https://mosher.mine.nu/xmlns/pub/";

    public static void main(final String... args) throws IOException, XMPException, GeneralSecurityException, Gnopt.InvalidOption {
        val opts = Gnopt.process(SecurePdfCli.class, args);
        if (opts.help) {
            return;
        }
        if (!opts.valid()) {
            log.error("Missing arguments. For help: SecurePdf --help");
            return;
        }

        val xmp = XMPMetaFactory.parse(new FileInputStream(opts.base+".xmp"));
        log.info("XMP: {}", xmp.getObjectName());
        val props = xmp.iterator(new IteratorOptions().setJustLeafnodes(true));
        while (props.hasNext()) {
            val prop = (XMPPropertyInfo)props.next();
            log.info("    {}.{}={}", prop.getNamespace(), prop.getPath(), prop.getValue());
        }
        val creator = Optional.ofNullable(xmp.getArrayItem(XMPConst.NS_DC, "creator", 1));
        if (creator.isPresent()) {
            log.info("dc:creator={}", creator.get().getValue());
        }
        val title = Optional.ofNullable(xmp.getArrayItem(XMPConst.NS_DC, "title", 1));
        if (title.isPresent()) {
            log.info("dc:title={}", title.get().getValue());
        }
        val location = Optional.ofNullable(xmp.getProperty(NS_PUB, "location"));
        if (location.isPresent()) {
            log.info("pub:location={}", location.get().getValue());
        }

        val pages = new TreeMap<String, Path>();
        addPagesTo(Path.of(opts.base), pages);

        val image = new Image(ImageDataFactory.create(pages.firstEntry().getValue().toUri().toURL())).scale(SCALE, SCALE);
        val size = new Rectangle(image.getImageScaledWidth(), image.getImageScaledHeight());

        try (val pdf = new PdfDocument(new PdfWriter(opts.base+".tmp", new WriterProperties().addXmpMetadata().setPdfVersion(PdfVersion.PDF_1_7)))) {
            log.info("creating PDF file...");
            setPrefsOf(pdf, creator, title);
            for (val e : pages.entrySet()) {
                addImagePage(e.getValue(), pdf);
            }
            log.info("page count: {}", pdf.getNumberOfPages());
            pdf.setXmpMetadata(xmp);
        }

        val loc = (location.isPresent() ? location.get().getValue() : opts.location);
        val author = (creator.isPresent() ? creator.get().getValue() : "The authors");
        sign(size, opts.height, author, loc, opts.graphic, opts.page, opts.keystore, opts.keystorePassword, opts.base+".tmp", opts.base+".pdf");
    }

    private static void setPrefsOf(final PdfDocument pdf, Optional<XMPProperty> author, Optional<XMPProperty> title) {
//        val prefs = new PdfViewerPreferences();
//        prefs.setFitWindow(true);
//        prefs.setHideMenubar(false);
//        prefs.setHideToolbar(false);
//        prefs.setHideWindowUI(false);
//        prefs.setCenterWindow(true);
//        prefs.setDisplayDocTitle(true);
//        prefs.setDuplex(SIMPLEX);
//        prefs.setNonFullScreenPageMode(USE_OUTLINES);
//        prefs.setPrintScaling(NONE);
//        pdf.getCatalog().setViewerPreferences(prefs);

//        val xmp = XMPMetaFactory.create();
//        xmp.setProperty();
//        pdf.setXmpMetadata(xmp);

        if (author.isPresent()) {
            pdf.getDocumentInfo().setAuthor(author.get().getValue());
        }
        if (title.isPresent()) {
            pdf.getDocumentInfo().setTitle(title.get().getValue());
        }
    }

    private static void addPagesTo(final Path dir, final Map<String, Path> pages) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    if (pages.containsKey(file.getFileName().toString())) {
                        log.warn("duplicate page file names, dropping: {}", file);
                    } else {
                        pages.put(file.getFileName().toString(), file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void addImagePage(final Path pathImage, final PdfDocument pdf) throws MalformedURLException {
        log.info("adding: {}", pathImage);
        val image = new Image(ImageDataFactory.create(pathImage.toUri().toURL())).scale(SCALE, SCALE);
        val size = new Rectangle(image.getImageScaledWidth(), image.getImageScaledHeight());
        log.info("    image size: {}", size);
        val page = pdf.addNewPage(new PageSize(size));
        try (val canvas = new Canvas(new PdfCanvas(page), size)) {
            canvas.add(image);
        }
    }

    private static void sign(final Rectangle size, float height, String creator, String location, String graphic, int page, String keystore, String password, String source, String dest) throws GeneralSecurityException, IOException {
        val provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        val ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(keystore), password.toCharArray());
        val alias = ks.aliases().nextElement();
        val pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
        val chain = ks.getCertificateChain(alias);

        val signer = new PdfSigner(
            new PdfReader(source),
            new FileOutputStream(dest),
            System.getProperty("java.io.tmpdir"),
            new StampingProperties());
        signer.setCertificationLevel(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED);
        signer.setFieldName("signature");

        // Create the signature appearance
        signer.getSignatureAppearance()
            .setSignatureCreator("iText "+ITextCoreProductData.getInstance().getVersion())
            .setReason("creator "+creator)
            .setContact(creator)
            .setLocation(location)
            .setSignatureGraphic(ImageDataFactory.create(graphic))
            .setLayer2Font(PdfFontFactory.createFont(StandardFontFamilies.HELVETICA))
            .setLayer2FontSize(6.0f)
            .setLayer2FontColor(new DeviceRgb(101, 123, 131)) // solarized:base00
            .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION)
            .setPageRect(new Rectangle(50.0f, 140.0f, size.getWidth()-100.0f, height))
            .setPageNumber(page);

        signer.signDetached(
            new BouncyCastleDigest(),
            new PrivateKeySignature(pk, DigestAlgorithms.SHA384, provider.getName()),
            chain,
            List.of(new CrlClientOnline(chain)),
            new OcspClientBouncyCastle(null),
            new TSAClientBouncyCastle("http://timestamp.identrust.com"),
            0,
            PdfSigner.CryptoStandard.CMS);
    }
}
