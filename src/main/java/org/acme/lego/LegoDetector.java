package org.acme.lego;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.memory.enums.DebugMode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.opencv.core.Core;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class LegoDetector {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LegoDetector.class);

    private static final int height = 224;
    private static final int width = 224;
    private static final int channels = 3;

    private final static DataNormalization scaler = new VGG16ImagePreProcessor();
    private final static NativeImageLoader loader = new NativeImageLoader(height, width, channels);

    public static void main(String[] args) throws IOException {
        long bootTime = System.currentTimeMillis();
        Nd4j.getWorkspaceManager().setDebugMode(DebugMode.SPILL_EVERYTHING);

        //Import vgg
        //Note that the model imported does not have an output layer (check printed summary)
        //  nor any training related configs (model from keras was imported with only weights and json)
        ComputationGraph vgg16 = ComputationGraph.load(new File("lego_model.zip"), false);

        System.out.println("boot time : " + (System.currentTimeMillis()-bootTime));

        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3020/y0x0_1.png");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3623/y0x0_1.png");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3002_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3004_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3020_1.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3020_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3022_1.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3623_1.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3002_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3004_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3020_1.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3020_2.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3022_1.jpg");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3623_1.jpg");
    }

    private static void test(ComputationGraph vgg16, String path) throws IOException {
        System.out.println(path);
        long startTime = System.currentTimeMillis();
        File file = new File(path);
        INDArray image = loader.asMatrix(file);

        scaler.transform(image);

        System.out.println("load elapsed : " + (System.currentTimeMillis()- startTime));

        INDArray[] output = vgg16.output(false, image);

        System.out.println("time elapsed : " + (System.currentTimeMillis()- startTime));
        String decodePredictions = decodePredictions(output[0]);
        System.out.println(decodePredictions);
        System.out.println("time elapsed : " + (System.currentTimeMillis()- startTime));
    }

    public static String decodePredictions(INDArray predictions) {
        Scanner reader;
        try {
            reader = new Scanner(new File("lego_model_labels.csv"));
        } catch (FileNotFoundException e) {
            throw  new IllegalStateException(e);
        }
        String nextLine = reader.nextLine();

        String[] labels =  nextLine.split(",");
        String predictionDescription = "";
        int[] top3 = new int[3];
        float[] top3Prob = new float[3];

        //brute force collect top 3
        int i = 0;
        for (int batch = 0; batch < predictions.size(0); batch++) {
            predictionDescription += "Predictions: ";
            if (predictions.size(0) > 1) {
                predictionDescription += String.valueOf(batch);
            }
            predictionDescription += " :";
            INDArray currentBatch = predictions.getRow(batch).dup();
            while (i < 3) {
                top3[i] = Nd4j.argMax(currentBatch, 1).getInt(0, 0);
                top3Prob[i] = currentBatch.getFloat(batch, top3[i]);
                currentBatch.putScalar(0, top3[i], 0);
                predictionDescription += "\n\t" + String.format("%3f", top3Prob[i] * 100) + "%, " + labels[(top3[i])];
                i++;
            }
        }
        return predictionDescription;
    }
}
