package org.acme.lego.preprocessing;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ImagePreProcessor {
    static {
        nu.pattern.OpenCV.loadLocally();
    }
    public static final int FILES_TO_KEEP = 1500;
    public static final String BASE_FOLDER = "povray/";

    public static final String POVRAY_CROPPED = BASE_FOLDER + "cropped/";

    private final boolean debug = false;

    private final Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
    private final Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));

    private final Map<String, AtomicInteger> counterPerObject = new ConcurrentHashMap<>();
    private final Map<String, Double> sizePerFile = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new ImagePreProcessor().start();
    }

    private void start() {
        File baseFolder = new File(BASE_FOLDER);
        getStreamOfObjectFolders(baseFolder)
                .limit(100)
                .peek(p -> new File(POVRAY_CROPPED + p.getName()).mkdirs())
                .forEach(folder -> {
                    System.gc();
                    sizePerFile.clear();
                    List<FileAndSize> fileAndSizes = new ArrayList<>(getStreamOfImagesInFolder(folder)
                            .parallel()
                            .peek(this::testProcess)
                            .filter(file -> sizePerFile.get(file.getAbsolutePath()) != null)
                            .map(file -> {
                                double size = sizePerFile.get(file.getAbsolutePath());
                                return new FileAndSize(file, size);
                            })
                            .sorted(Comparator.comparingDouble(FileAndSize::size))
                            .toList());

                    if (fileAndSizes.size() > FILES_TO_KEEP) {
                        fileAndSizes.subList(0, fileAndSizes.size() - FILES_TO_KEEP)
                                .stream()
                                .map(FileAndSize::file)
                                .forEach(file -> {
                                    File croppedFile = new File(POVRAY_CROPPED + file.getParentFile().toPath().getFileName() + "/" + file.toPath().getFileName());
                                    System.err.println("Remove " + croppedFile.getAbsolutePath());
                                    croppedFile.delete();
                                });
                    }
                });


        counterPerObject.forEach((key, value) -> System.out.println(key + ": " + value.get()));
    }

    private Stream<File> getStreamOfImagesInFolder(File folder) {
        return Stream.of(Objects.requireNonNull(folder.listFiles()))
                .filter(f -> f.isFile() && f.getName().endsWith(".png"));
    }

    private Stream<File> getStreamOfObjectFolders(File baseFolder) {
        return Stream.of(Objects.requireNonNull(baseFolder.listFiles()))
                .filter(File::isDirectory)
                .filter(f -> !f.getName().equals("cropped"))
                .filter(f -> !f.getName().equals("old"))
                .filter(f -> !f.getName().equals("colors"))
                .filter(f -> !f.getName().equals("pov"));
    }

    private void testProcess(File f) {
        testProcess(new MatFile(Imgcodecs.imread(f.getAbsolutePath()), f));
    }

    private void testProcess(MatFile matFile) {
        var output = preProcess(matFile);
        if (output == null) {
            System.err.println("Failed to process: " + matFile.file.getAbsolutePath());
        } else {
            System.out.println("Processed: " + matFile.file.getAbsolutePath());
        }
        matFile.mat.release();
    }

    public Mat preProcess(MatFile matFile) {
        Mat image = matFile.mat();

        Mat hsvImage = getCleanedupHsvImage(image);

        double[] bgColor = determineBackgroundColor(hsvImage);
        // remember: H ranges 0-180, S and V range 0-255

        final int bgColorRange = 3;
        Scalar minValues = new Scalar(Math.min(0, bgColor[0] - bgColorRange), Math.min(0, bgColor[1] - bgColorRange), Math.min(0, bgColor[2] - bgColorRange));
        Scalar maxValues = new Scalar(Math.min(180, bgColor[0] + bgColorRange), Math.min(255, bgColor[1] + bgColorRange), Math.min(255, bgColor[2] + bgColorRange));

        Mat mask = new Mat();
        // threshold HSV
        Core.inRange(hsvImage, minValues, maxValues, mask);

        // invert mask
        Core.bitwise_not(mask, mask);

        Mat morphOutput = getCleanedUpMat(mask);

        mask.release();
        hsvImage.release();

        Rect rect = getRect(matFile.file, morphOutput);

        if (rect != null) {
            if (Math.min(rect.x, rect.y) == 0 || rect.x + rect.width == image.cols() || rect.y + rect.height == image.rows()) {
                return null;
            }

            // add padding to rect
            rect = addPadding(image, rect, 50);
            // rect must be min size of 224x224
            if (rect.width < 224) {
                // new rect of 224x224 with center at center of old rect
                rect = new Rect(rect.x - (224 - rect.width) / 2, rect.y - (224 - rect.height) / 2, 224, 224);
            }

            if (debug) {
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(250, 0, 0));
                HighGui.imshow("result", image);
                HighGui.waitKey(1000);
            }

            // check if the rect is valid and not our of bounds
            if (Math.min(rect.x, rect.y) < 0 || rect.x + rect.width > image.cols() || rect.y + rect.height > image.rows()) {
                return null;
            }
            // crop image
            Mat croppedImage = new Mat(image, rect);
            // resize image
            Mat resizedImage = new Mat();
            Imgproc.resize(croppedImage, resizedImage, new Size(224, 224));
            AtomicInteger counter = counterPerObject.computeIfAbsent(matFile.file.getParentFile().getName(), k -> new AtomicInteger());
            counter.getAndIncrement();

            // write cropped image to file
            Imgcodecs.imwrite(POVRAY_CROPPED + matFile.file.getParentFile().getName() + "/" + matFile.file.getName(), resizedImage);
            croppedImage.release();
            resizedImage.release();
            return image;
        }

        return null;
    }

    private Mat getCleanedupHsvImage(Mat image) {
        Mat hsvImage = new Mat();
        Mat blurredImage = new Mat();
        // remove some noise
        Imgproc.blur(image, blurredImage, new Size(28, 28));

        // convert the frame to HSV
        Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);
        blurredImage.release();
        return hsvImage;
    }

    private Mat getCleanedUpMat(Mat mask) {
        Mat temp1 = new Mat();
        Mat temp2 = new Mat();

        Imgproc.erode(mask, temp1, erodeElement);

        Imgproc.dilate(temp1, temp2, dilateElement);
        Imgproc.dilate(temp2, temp1, dilateElement);

        temp2.release();
        return temp1;
    }

    private Rect getRect(File file, Mat morphOutput) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        // find contours
        Imgproc.findContours(morphOutput, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        return findInContours(hierarchy, contours, file);
    }

    private Rect addPadding(Mat image, Rect rect, final int padding) {
        rect = new Rect(rect.x - padding / 2, rect.y - padding / 2, rect.height + padding / 2, rect.height + padding / 2);
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
        return rect;
    }

    private double[] determineBackgroundColor(Mat blurredImage) {
        byte[] topLeft = new byte[3];
        blurredImage.get(1, 1, topLeft);

        byte[] topRight = new byte[3];
        blurredImage.get(1, blurredImage.cols() - 1, topRight);

        byte[] bottomRight = new byte[3];
        blurredImage.get(blurredImage.rows() - 1, blurredImage.cols() - 1, bottomRight);

        byte[] bottomLeft = new byte[3];
        blurredImage.get(blurredImage.rows() - 1, 1, bottomLeft);

        return new double[]{
                removeOutliersAndGetAverage(topLeft[0], topLeft[0], bottomLeft[0], bottomRight[0]),
                removeOutliersAndGetAverage(topLeft[1], topLeft[1], bottomLeft[1], bottomRight[1]),
                removeOutliersAndGetAverage(topLeft[2], topLeft[2], bottomLeft[2], bottomRight[2])};
    }

    public static double removeOutliersAndGetAverage(byte... bytes) {
        final int threshold = 8;
        // Calculate the average value of the byte array
        double avg = 0;
        for (byte b : bytes) {
            short s = (short) (b & 0xFF); // Upcast to short
            avg += s;
        }
        avg /= bytes.length;

        // Remove values that are not close to the average
        List<Short> list = new ArrayList<>();
        for (byte b : bytes) {
            short s = (short) (b & 0xFF); // Upcast to short
            if (Math.abs(s - avg) <= threshold) {
                list.add(s);
            }
        }

        // Calculate the average value of the remaining values
        double newAvg = 0;
        for (short s : list) {
            newAvg += s;
        }
        newAvg /= list.size();

        return newAvg;
    }

    private Rect findInContours(Mat hierarchy, List<MatOfPoint> contours, File file) {
        //drawContours(hierarchy, contours);
        if (contours.isEmpty()) {
            return null;
        }
        int largestContourIdx = 0;
        // find the biggest contour
        double largestArea = 0;
        for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
            double area = Imgproc.contourArea(contours.get(idx));
            if (area > largestArea) {
                largestArea = area;
                largestContourIdx = idx;
            }
        }
        sizePerFile.put(file.getAbsolutePath(), largestArea);
        MatOfPoint contour = contours.get(largestContourIdx);
        Rect rect = Imgproc.boundingRect(contour);
        hierarchy.release();
        return squareRectangleAndCenter(rect);
    }

    private void drawContours(Mat hierarchy, List<MatOfPoint> contours, Mat frame) {
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
            }
        }
    }

    private Rect squareRectangleAndCenter(Rect rect) {
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
        return new Rect(x, y, width, height);
    }

    private record MatFile(Mat mat, File file) {
    }

    private record FileAndSize(File file, double size) {
    }

}