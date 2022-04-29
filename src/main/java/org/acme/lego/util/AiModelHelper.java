package org.acme.lego.util;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AiModelHelper {

    public final static String LABEL_FILE_NAME = "lego_model_labels.csv";
    public final static String MODEL_FILE_NAME = "lego_model.zip";

    public static final int height = 224;
    public static final int width = 224;
    public static final int channels = 3;

    private final String[] labels;

    // VGG16
    //private final static DataNormalization PRE_PROCESSOR = new VGG16ImagePreProcessor();
    // RESNET
    public final static DataNormalization PRE_PROCESSOR = new ImagePreProcessingScaler();

    public AiModelHelper(File folder) {
        this.labels = loadLabels(folder);
    }

    private String[] loadLabels(File folder) {
        Scanner reader;
        try {
            reader = new Scanner(new File(folder, LABEL_FILE_NAME));
        } catch (FileNotFoundException e) {
            throw  new IllegalStateException(e);
        }
        String nextLine = reader.nextLine();

        return nextLine.split(",");
    }

    public List<Prediction> decodePredictions(INDArray predictions) {
        List<Prediction> predictionList = new ArrayList<>();
        int[] top3 = new int[3];
        float[] top3Prob = new float[3];

        //brute force collect top 3
        int i = 0;
        for (int batch = 0; batch < predictions.size(0); batch++) {
            INDArray currentBatch = predictions.getRow(batch).dup();
            while (i < 3) {
                top3[i] = Nd4j.argMax(currentBatch, 1).getInt(0, 0);
                top3Prob[i] = currentBatch.getFloat(batch, top3[i]);
                currentBatch.putScalar(0, top3[i], 0);
                predictionList.add(new Prediction(labels[(top3[i])], top3Prob[i] * 100));
                //predictionDescription += "\n\t" + String.format("%3f", top3Prob[i] * 100) + "%, " + labels[(top3[i])];
                i++;
            }
        }
        return predictionList;
    }
}
