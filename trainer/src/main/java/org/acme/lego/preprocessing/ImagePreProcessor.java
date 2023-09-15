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
//    public static final String BASE_FOLDER = "../povray/";
    public static final String BASE_FOLDER = "samples/cam";

    public static final String POVRAY_CROPPED = BASE_FOLDER + "cropped/";

    private final boolean debug = false;
    private final boolean blur = false;
    private final boolean gray = true;

    private final AtomicInteger failures = new AtomicInteger(0);

    private final static Random random = new Random();

    private final Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 20));
    private final Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));

    private final Map<String, AtomicInteger> counterPerObject = new ConcurrentHashMap<>();
    private final Map<String, Double> sizePerFile = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new ImagePreProcessor().start();
        System.out.println("Total processing time took: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void start() {
        File baseFolder = new File(BASE_FOLDER);
        getStreamOfObjectFolders(baseFolder)
                .peek(p -> new File(POVRAY_CROPPED + p.getName()).mkdirs())
                .forEach(folder -> {
                    System.gc();
                    sizePerFile.clear();
                    List<FileAndSize> fileAndSizes = new ArrayList<>(getStreamOfImagesInFolder(folder)
                            .parallel()
//                            .limit(1)
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
        System.out.println("Failures: " + failures.get());
    }

    private Stream<File> getStreamOfImagesInFolder(File folder) {
        return Stream.of(Objects.requireNonNull(folder.listFiles()))
                .filter(f -> f.isFile() && (f.getName().endsWith(".png") || f.getName().endsWith(".jpg")));
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
        var output = process(matFile);
        if (output == null) {
            System.err.println("Failed to process: " + matFile.file.getAbsolutePath());
            sizePerFile.remove(matFile.file.getAbsolutePath());
            failures.incrementAndGet();
        } else {
            System.out.println("Processed: " + matFile.file.getAbsolutePath());
        }
        matFile.mat.release();
    }

    public Mat process(MatFile matFile) {
        Mat image = matFile.mat();

        Mat hsvImage = getCleanedupHsvImage(image);

//        double[] bgColor = determineBackgroundColor(hsvImage);
        double[] minBgColor = getMinColor(hsvImage);
        double[] maxBgColor = getMaxColor(hsvImage);
        // remember: H ranges 0-180, S and V range 0-255

        if (debug) {
            System.out.println("min BG color: " + Arrays.toString(minBgColor));
            System.out.println("max BG color: " + Arrays.toString(maxBgColor));
            HighGui.imshow("hsvImage", hsvImage);
            HighGui.waitKey(1000);
        }

        final int hColorRange = 90;
        final int sColorRange = 30;
        final int vColorRange = 30;
        Scalar minValues = new Scalar(Math.min(0, minBgColor[0] - hColorRange), Math.min(0, minBgColor[1] - sColorRange), Math.min(0, minBgColor[2] - vColorRange));
        Scalar maxValues = new Scalar(Math.min(180, maxBgColor[0] + hColorRange), Math.min(255, maxBgColor[1] + sColorRange), Math.min(255, maxBgColor[2] + vColorRange));

        Mat mask = new Mat();
        // threshold HSV
        Core.inRange(hsvImage, minValues, maxValues, mask);

        // invert mask
        Core.bitwise_not(mask, mask);

        if (debug) {
            HighGui.imshow("mask", mask);
            HighGui.waitKey(1000);
        }

        Mat morphOutput = getCleanedUpMat(mask);

        mask.release();
        hsvImage.release();

        Rect rect = getRect(matFile, morphOutput);

        if (rect != null) {
            if (debug) {
                System.out.println("rect " + new Point(rect.x, rect.y) + " " + new Point(rect.x + rect.width, rect.y + rect.height));
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(250, 0, 0));
                HighGui.imshow("rect", image);
                HighGui.waitKey(1000);
            }
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

            int blurSize = random.nextInt(3);
            Mat blurredImage;
            if (blurSize > 1 && blur) {
                blurredImage = new Mat();
                Imgproc.blur(resizedImage, blurredImage, new Size(blurSize, blurSize));
            } else {
                blurredImage = resizedImage;
            }

            // lower contrast:
            Mat temp3 = new Mat(blurredImage.rows(), blurredImage.cols(), blurredImage.type());
            blurredImage.convertTo(temp3, -1, 1.5, -75);
            blurredImage.release();

            Mat grayImage = new Mat();
            if (gray) {
                Imgproc.cvtColor(temp3, grayImage, Imgproc.COLOR_RGB2GRAY);
                temp3.release();
            } else {
                grayImage = temp3;
            }

            // write cropped image to file
            Imgcodecs.imwrite(POVRAY_CROPPED + matFile.file.getParentFile().getName() + "/" + matFile.file.getName(), grayImage);
            croppedImage.release();
            resizedImage.release();
            blurredImage.release();
            grayImage.release();
            return image;
        } else if (debug) {
            System.out.println("No rect found");
        }

        return null;
    }

    private Mat getCleanedupHsvImage(Mat image) {
        Mat hsvImage = new Mat();
        Mat blurredImage = new Mat();
        // remove some noise
        Imgproc.blur(image, blurredImage, new Size(12, 12));

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

    private Rect getRect(MatFile file, Mat morphOutput) {
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

    private ConorColors getConorColors(Mat image) {
        final int position = 250;
        Mat blurredImage = new Mat();
        // remove some more noise
        Imgproc.blur(image, blurredImage, new Size(200, 200));

        byte[] topLeft = new byte[3];
        blurredImage.get(position, position, topLeft);

        byte[] topRight = new byte[3];
        blurredImage.get(position, blurredImage.cols() - position, topRight);

        byte[] bottomRight = new byte[3];
        blurredImage.get(blurredImage.rows() - position, blurredImage.cols() - position, bottomRight);

        byte[] bottomLeft = new byte[3];
        blurredImage.get(blurredImage.rows() - position, position, bottomLeft);

        return new ConorColors(topLeft, topRight, bottomLeft, bottomRight);
    }

    private double[] getMinColor(Mat image) {
        ConorColors conorColors = getConorColors(image);

        return new double[]{
                Stream.of(conorColors.topLeft[0], conorColors.topRight[0], conorColors.bottomLeft[0], conorColors.bottomRight[0]).mapToDouble(b -> (b & 0xFF)).min().getAsDouble(),
                Stream.of(conorColors.topLeft[1], conorColors.topRight[1], conorColors.bottomLeft[1], conorColors.bottomRight[1]).mapToDouble(b -> (b & 0xFF)).min().getAsDouble(),
                Stream.of(conorColors.topLeft[2], conorColors.topRight[2], conorColors.bottomLeft[2], conorColors.bottomRight[2]).mapToDouble(b -> (b & 0xFF)).min().getAsDouble()
        };
    }

    private double[] getMaxColor(Mat image) {
        ConorColors conorColors = getConorColors(image);

        return new double[]{
                Stream.of(conorColors.topLeft[0], conorColors.topRight[0], conorColors.bottomLeft[0], conorColors.bottomRight[0]).mapToDouble(b -> (b & 0xFF)).max().getAsDouble(),
                Stream.of(conorColors.topLeft[1], conorColors.topRight[1], conorColors.bottomLeft[1], conorColors.bottomRight[1]).mapToDouble(b -> (b & 0xFF)).max().getAsDouble(),
                Stream.of(conorColors.topLeft[2], conorColors.topRight[2], conorColors.bottomLeft[2], conorColors.bottomRight[2]).mapToDouble(b -> (b & 0xFF)).max().getAsDouble()
        };
    }

    private double[] determineBackgroundColor(Mat image) {
        ConorColors conorColors = getConorColors(image);

        return new double[]{
                removeOutliersAndGetAverage(conorColors.topLeft[0], conorColors.topRight[0], conorColors.bottomLeft[0], conorColors.bottomRight[0]),
                removeOutliersAndGetAverage(conorColors.topLeft[1], conorColors.topRight[1], conorColors.bottomLeft[1], conorColors.bottomRight[1]),
                removeOutliersAndGetAverage(conorColors.topLeft[2], conorColors.topRight[2], conorColors.bottomLeft[2], conorColors.bottomRight[2])};
    }

    public static double removeOutliersAndGetAverage(byte... bytes) {
        int threshold = 8;
        // Calculate the average value of the byte array
        double avg = 0;
        for (byte b : bytes) {
            short s = (short) (b & 0xFF); // Upcast to short
            avg += s;
        }
        avg /= bytes.length;

        double outcome = Double.NaN;
        while (Double.isNaN(outcome)) {
            outcome = getAvgWithoutOutlires(threshold, avg, bytes);
            threshold += 5;
        }
        return outcome;
    }

    private static double getAvgWithoutOutlires(int threshold, double avg, byte[] bytes) {
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

    private Rect findInContours(Mat hierarchy, List<MatOfPoint> contours, MatFile file) {
        if (debug) {
            drawContours(hierarchy, contours, file.mat);
        }
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
        sizePerFile.put(file.file.getAbsolutePath(), largestArea);
        MatOfPoint contour = contours.get(largestContourIdx);
        Rect rect = Imgproc.boundingRect(contour);
        hierarchy.release();
        return squareRectangleAndCenter(rect);
    }

    private void drawContours(Mat hierarchy, List<MatOfPoint> contours, Mat frame) {
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0), 10);
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

    private record ConorColors(byte[] topLeft, byte[] topRight, byte[] bottomLeft, byte[] bottomRight) {
    }

}