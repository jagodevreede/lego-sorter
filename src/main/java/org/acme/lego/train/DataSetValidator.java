package org.acme.lego.train;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Util that removes images from data set that contain no blocks. or block that are barely visible.
 */
public class DataSetValidator {

    public static void main(String[] args) {
        Loader.load(opencv_java.class);
        new DataSetValidator().start();
    }

    private void start() {
        File baseFolder = new File(LegoDataSetIterator.DATA_DIR);
        Stream.of(Objects.requireNonNull(baseFolder.listFiles()))
                .filter(File::isDirectory)
                .flatMap(p -> Stream.of(Objects.requireNonNull(p.listFiles()))
                        .filter(f -> f.isFile() && f.getName().endsWith(".png")))
                .forEach(this::testImg);
    }

    private boolean testImg(File file) {
        Mat frame = Imgcodecs.imread(file.getAbsolutePath());
        Mat blurredImage = new Mat();
        Mat hsvImage = new Mat();
        Mat mask = new Mat();
        Mat morphOutput = new Mat();

// remove some noise
        Imgproc.blur(frame, blurredImage, new Size(7, 7));

// convert the frame to HSV
        Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

        // get thresholding values from the UI
// remember: H ranges 0-180, S and V range 0-255
        Scalar minValues = new Scalar(2, 10, 30);
        Scalar maxValues = new Scalar(180, 255, 255);


// threshold HSV image to select tennis balls
        Core.inRange(hsvImage, minValues, maxValues, mask);

        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

        Imgproc.erode(mask, morphOutput, erodeElement);
        Imgproc.erode(mask, morphOutput, erodeElement);

        Imgproc.dilate(mask, morphOutput, dilateElement);
        Imgproc.dilate(mask, morphOutput, dilateElement);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

// find contours
        Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

// if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
        {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
            {
                Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
            }
        }

        if (contours.size() > 0) {
            //HighGui.waitKey(1);
        } else {
            System.out.println("Found bad one: " + file.getAbsolutePath());
            file.delete();
            HighGui.imshow("result", frame);
            HighGui.waitKey(100);
        }

        return true;
    }
}
