package org.acme.lego;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.acme.lego.train.Learning;
import org.acme.lego.validate.ModelVerification;

@QuarkusMain
public class MainEntry implements QuarkusApplication {

    static {
        String osArch = System.getProperty("os.arch");
        if ("aarch64".equals(osArch)) {
            System.out.println("Running on " + osArch + " loading local opencv");
            System.loadLibrary("opencv_java470");
        }
    }

    @Override
    public int run(String... args) throws Exception {
        if (args.length != 1) {
            System.err.println("Need an argument [t]rain or [v]alidate");
        } else switch (args[0].toString()) {
            case "t":
                Learning.main(new String[] {});
                break;
            case "v":
                ModelVerification.main(new String[] {});
                break;
            default:
                System.err.println("Unknown argument: Need an argument [t]rain or [v]alidate");
        }
        return 0;
    }
}
