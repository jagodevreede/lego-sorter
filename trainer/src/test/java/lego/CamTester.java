package lego;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.highgui.HighGui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;

@Slf4j
public class CamTester {
    public static final String CAPTURE_URL = "http://192.168.178.140/capture";
    public static final File SAMPLES_FOLDER = new File("./samples");

    public static void main(String[] args) {
        log.info("Starting");
        SAMPLES_FOLDER.mkdirs();
        Loader.load(opencv_java.class);
        log.info("Loading opencv done");
        while (true) {
            Mat mat = getImage();

            Mat cropped = crop(mat);

            imwrite(new File(SAMPLES_FOLDER, System.currentTimeMillis() + ".png").getAbsolutePath(), cropped);

            HighGui.imshow("Cam1", cropped);
            HighGui.waitKey(1000);

        }
    }

    private static Mat crop(Mat mat) {
        return new Mat(mat, new Rect(48, 8, 224, 224));
    }

    private static Mat getImage() {
        final long startTime = System.currentTimeMillis();
        BufferedImage image;
        Mat mat = null;
        try {
            URL url = new URL(CAPTURE_URL);
            image = ImageIO.read(url);
            mat = bufferedImage2Mat(image);
            log.info("Loaded image w{}h{} in {}ms", image.getWidth(), image.getHeight(), System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mat;
    }

    public static Mat bufferedImage2Mat(BufferedImage image) throws IOException {
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }
}
