package org.acme.lego.images;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ImagePreProcessor {

    private final Map<String, AtomicInteger> counterPerObject = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Loader.load(opencv_java.class);
        new ImagePreProcessor().start();
    }

    private void start() {
        File baseFolder = new File("../povray/processed");
        Stream.of(Objects.requireNonNull(baseFolder.listFiles()))
                .filter(File::isDirectory)
                //.filter(f -> f.getName().equals("3006"))
                .flatMap(p -> Stream.of(Objects.requireNonNull(p.listFiles()))
                        .filter(f -> f.isFile() && f.getName().endsWith(".png")))
                .map(f -> {
                    // load map
                    //System.out.println("Loading: " + f.getAbsolutePath());
                    return new MatFile(Imgcodecs.imread(f.getAbsolutePath()), f);
                })
                 .parallel()
                .forEach(this::testProcess);

        counterPerObject.forEach((key, value) -> System.out.println(key + ": " + value.get()));
    }

    private void testProcess(MatFile matFile) {
        var output = preProcess(matFile);
        if (output == null) {
            System.err.println("Failed to process: " + matFile.file.getName());
        } else {
            System.out.println("Processed: " + matFile.file.getName());
        }
    }

    public Mat preProcess(MatFile matFile) {
        Mat image = matFile.mat();

        Mat blurredImage = new Mat();
        Mat hsvImage = new Mat();
        Mat mask = new Mat();
        Mat morphOutput = new Mat();

        // remove some noise
        //Imgproc.blur(image, blurredImage, new Size(7, 7));
        Imgproc.blur(image, blurredImage, new Size(28, 28));

// convert the frame to HSV
        Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

        // get thresholding values from the UI
// remember: H ranges 0-180, S and V range 0-255
//        Scalar minValues = new Scalar(5, 10, 30);
//        Scalar maxValues = new Scalar(180, 150, 250);
        Scalar minValues = new Scalar(0, 20, 0);
        Scalar maxValues = new Scalar(180, 255, 40);

// threshold HSV image to select tennis balls
        Core.inRange(hsvImage, minValues, maxValues, mask);

        // invert mask
        Core.bitwise_not(mask, mask);

        //Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        //Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));

        Imgproc.erode(mask, morphOutput, erodeElement);
        Imgproc.erode(mask, morphOutput, erodeElement);

        Imgproc.dilate(mask, morphOutput, dilateElement);
        Imgproc.dilate(mask, morphOutput, dilateElement);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

// find contours
        Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        Rect rect = writeContours(hierarchy, contours, image);
//
//                    HighGui.imshow("result", mask);
//            HighGui.waitKey(100);
        if (rect == null) {
//            HighGui.imshow("result", image);
//            HighGui.waitKey(0);
        } else {
            if (Math.min(rect.x, rect.y) == 0 || rect.x + rect.width == image.cols() || rect.y + rect.height == image.rows()) {
                return null;
            }
            // add padding to rect
           /* rect = new Rect(rect.x - PADDING / 2, rect.y - PADDING / 2, rect.height + PADDING / 2, rect.height + PADDING / 2);
            if (Math.min(rect.x, rect.y) < 0 || Math.max(rect.x + rect.width, rect.y + rect.height) > image.cols()) {
                if (rect.x < 0) {
                    int oldX = rect.x;
                    rect.x = 0;
                    rect.width = rect.width - oldX;
                }
                if (rect.y < 0) {
                    int oldY = rect.y;
                    rect.y = 0;
                    rect.height = rect.height - oldY;
                }
                if (rect.x + rect.width > image.cols()) {
                    int subtract = rect.x + rect.width - image.cols();
                    rect.x = rect.x - subtract;
                }
                if (rect.y + rect.height > image.rows()) {
                    int subtract = rect.y + rect.height - image.rows();
                    rect.y = rect.y - subtract;
                }
            }
            // rect must be min size of 244x244
            if (rect.width < 244) {
                // new rect of 244x244 with center at center of old rect
                rect = new Rect(rect.x - (244 - rect.width) / 2, rect.y - (244 - rect.height) / 2, 244, 244);
            }*/
            // rect must be min size of width of image
            rect = new Rect(0, rect.y - (image.cols() - rect.width) / 2, image.cols(), image.cols());
            if (rect.y < 0) {
                rect.y = 0;
            } else if (rect.y + rect.height > image.rows()) {
                rect.y = image.rows() - rect.height;
            }
            // check if the rect is valid and not our of bounds
            if (Math.min(rect.x, rect.y) < 0 || rect.x + rect.width > image.cols() || rect.y + rect.height > image.rows()) {
//                HighGui.imshow("result", image);
//                HighGui.waitKey(100);
                return null;
            }
            // crop image
            Mat croppedImage = new Mat(image, rect);
            // resize image
            Mat resizedImage = new Mat();
            Imgproc.resize(croppedImage, resizedImage, new Size(244, 244));
//            HighGui.imshow("result", resizedImage);
//            HighGui.waitKey(100);
            AtomicInteger counter = counterPerObject.computeIfAbsent(matFile.file.getParentFile().getName(), k -> new AtomicInteger());
            counter.getAndIncrement();

            // write cropped image to file
            new File("./povray/cropped/" + matFile.file.getParentFile().getName()).mkdirs();
            Imgcodecs.imwrite("./povray/cropped/" + matFile.file.getParentFile().getName() + "/" + counter.get() + ".png", resizedImage);
        }

        return image;
    }

    private Rect writeContours(Mat hierarchy, List<MatOfPoint> contours, Mat frame) {
        // if any contour exist...
        /*if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
            }
        }*/
        if (contours.isEmpty()) {
            return null;
        }
        int largestContourIdx = 0;
        if (contours.size() > 1) {
            // find the biggest contour
            double largestArea = 0;
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                double area = Imgproc.contourArea(contours.get(idx));
                if (area > largestArea) {
                    largestArea = area;
                    largestContourIdx = idx;
                }
            }
        }
        MatOfPoint contour = contours.get(largestContourIdx);
        Rect rect = Imgproc.boundingRect(contour);
        // Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
        // square the rectangle and center it
        int width = rect.width;
        int height = rect.height;
        int x = rect.x;
        int y = rect.y;
        if (width < height) {
            x = x + (width - height) / 2;
            width = height;
        } else {
            y = y + (height - width) / 2;
            height = width;
        }
        // new rectangle
        rect = new Rect(x, y, width, height);

//        Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255));
        return rect;
    }

    private record MatFile(Mat mat, File file) {
    }
}
