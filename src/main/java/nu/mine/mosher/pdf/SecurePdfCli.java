package nu.mine.mosher.pdf;

import java.util.Optional;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class SecurePdfCli {
    boolean valid() {
        return this.base != null;
    }

    boolean help;
    String base;
    String graphic;
    String keystore;
    String keystorePassword = "";
    String location = "";
    int page = 1;
    float height = 40.0f;
    float left = 52.0f;
    float bottom = 130.0f;
    float margins = 0.0f;

    public void help(final Optional<String> v) {
        System.out.println("usage:");
        System.out.println("    SecurePdf [OPTIONS] base-name");
        System.out.println("options:");
        System.out.println("    --help");
        System.out.println("    --page");
        System.out.println("    --keystore");
        System.out.println("    --password");
        System.out.println("    --location");
        System.out.println("    --graphic");
        System.out.println("    --height");
        System.out.println("    --left");
        System.out.println("    --margins");
        this.help = true;
    }

    public void __(final Optional<String> v) {
        this.base = v.orElseThrow();
    }

    public void location(final Optional<String> v) {
        this.location = v.orElseThrow();
    }

    public void graphic(final Optional<String> v) {
        this.graphic = v.orElseThrow();
    }

    public void keystore(final Optional<String> v) {
        this.keystore = v.orElseThrow();
    }

    public void password(final Optional<String> v) {
        this.keystorePassword = v.orElseThrow();
    }

    public void page(final Optional<String> v) {
        this.page = Integer.parseInt(v.orElseThrow());
    }

    public void height(final Optional<String> v) {
        this.height = Float.parseFloat(v.orElseThrow());
    }

    public void left(final Optional<String> v) {
        this.left = Float.parseFloat(v.orElseThrow());
    }

    public void bottom(final Optional<String> v) {
        this.bottom = Float.parseFloat(v.orElseThrow());
    }

    public void margins(final Optional<String> v) {
        this.margins = Float.parseFloat(v.orElseThrow());
    }
}
