package nu.mine.mosher.pdf;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SecurePdfCli {
    boolean valid() {
        return this.base != null;
    }

    String base;
    String graphic;
    String keystore;
    String keystorePassword = "";
    String location = "";
    int page = 1;
    float height = 60.0f;

    public void help(final Optional<String> v) {
        System.out.println("usage:");
        System.out.println("    SecurePdf [OPTIONS] base-name");
        System.out.println("options:");
        System.out.println("    --page");
        System.out.println("    --keystore");
        System.out.println("    --password");
        System.out.println("    --location");
        System.out.println("    --graphic");
        System.out.println("    --height");
    }

    public void __(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.base = v.get();
    }

    public void location(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.location = v.get();
    }

    public void graphic(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.graphic = v.get();
    }

    public void keystore(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.keystore = v.get();
    }

    public void password(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.keystorePassword = v.get();
    }

    public void page(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.page = Integer.parseInt(v.get());
    }

    public void height(final Optional<String> v) {
        if (v.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.height = Float.parseFloat(v.get());
    }
}
