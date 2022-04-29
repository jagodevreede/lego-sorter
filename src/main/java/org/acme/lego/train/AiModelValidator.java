package org.acme.lego.train;

import org.acme.lego.util.AiModelHelper;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.acme.lego.util.AiModelHelper.*;

public class AiModelValidator {
    private final static File modelFolder = new File("./runs/resnet-b16/");

    private final AiModelHelper modelHelper = new AiModelHelper(modelFolder);
    private final ComputationGraph model;

    public static void main(String[] args) throws Exception {
        Loader.load(opencv_java.class);
        new AiModelValidator().start();
    }

    public AiModelValidator() throws IOException {
        this.model = ComputationGraph.load(new File(modelFolder, AiModelHelper.MODEL_FILE_NAME), false);;
        model.init();
    }

    private void start() {
        System.out.println("");
        File baseFolder = new File(LegoDataSetIterator.DATA_DIR);
        Stream.of(Objects.requireNonNull(baseFolder.listFiles()))
                .filter(File::isDirectory)
                .flatMap(p -> Stream.of(Objects.requireNonNull(p.listFiles()))
                        .filter(f -> f.isFile() && f.getName().endsWith(".png")))
                .forEach(this::testImg);   
    }

    private void testImg(File file) {
        final String actualLabel = file.getParentFile().getName();
        try {
            NativeImageLoader loader = new NativeImageLoader(height, width, channels);
            INDArray image = loader.asMatrix(file);

            PRE_PROCESSOR.transform(image);

            INDArray[] output = model.output(false, image);
            var predictions = modelHelper.decodePredictions(output[0]);
            String decodePredictions = predictions.stream().map(Record::toString)
                    .collect(Collectors.joining(", "));
            if (!actualLabel.equals(predictions.get(0).label())) {
                System.out.println("\n" + actualLabel + "\n\t" + decodePredictions);
                Mat frame = Imgcodecs.imread(file.getAbsolutePath());
                HighGui.imshow("result", frame);
                HighGui.waitKey(100);
            } else {
                //System.out.println("\n" + actualLabel + "\n\t" + decodePredictions);
                System.out.print(".");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
