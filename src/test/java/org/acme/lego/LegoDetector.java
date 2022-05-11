package org.acme.lego;

import org.acme.lego.util.AiModelHelper;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.memory.enums.DebugMode;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.acme.lego.util.AiModelHelper.*;

public class LegoDetector {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LegoDetector.class);

    private final static File modelFolder = new File("./");

    private final static DataNormalization scaler = new VGG16ImagePreProcessor();
    private final static AiModelHelper modelHelper = new AiModelHelper(modelFolder);

    public static void main(String[] args) throws IOException {
        long bootTime = System.currentTimeMillis();
        Nd4j.getWorkspaceManager().setDebugMode(DebugMode.SPILL_EVERYTHING);

        //Import vgg
        //Note that the model imported does not have an output layer (check printed summary)
        //  nor any training related configs (model from keras was imported with only weights and json)
        ComputationGraph vgg16 = ComputationGraph.load(new File(modelFolder, AiModelHelper.MODEL_FILE_NAME), false);

        System.out.println("boot time : " + (System.currentTimeMillis()-bootTime));

        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3623/3623-21.png");
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3004/3004-21.png");
        //     test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3623/y0x0_1.png");
        //      test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/bricks/3020/y0x0_1.png");
      /*  test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/3002_2.jpg");
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
        test(vgg16, "/Users/jagodevreede/git/lego-sorter/povray/real/real/3623_1.jpg");*/
    }

    private static void test(ComputationGraph vgg16, String path) throws IOException {
        vgg16.init();
        NativeImageLoader loader = new NativeImageLoader(height, width, channels);
        System.out.println(path);
        long startTime = System.currentTimeMillis();
        File file = new File(path);
        INDArray image = loader.asMatrix(file);

        scaler.transform(image);

        System.out.println("load elapsed : " + (System.currentTimeMillis()- startTime));

        INDArray[] output = vgg16.output(false, image);

        System.out.println("time elapsed : " + (System.currentTimeMillis()- startTime));
        String decodePredictions = modelHelper.decodePredictions(output[0]) .stream().map(Record::toString)
                .collect(Collectors.joining(", "));
        System.out.println(decodePredictions);
        System.out.println("time elapsed : " + (System.currentTimeMillis()- startTime));
    }
}
