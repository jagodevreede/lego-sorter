package org.acme.lego.train;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.VGG16;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AiModelTrainer {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AiModelTrainer.class);

    protected static final int numClasses = 5;
    protected static final long seed = 12345;

    private static final int trainPerc = 80;
    private static final int batchSize = 16;
    private static final String featureExtractionLayer = "fc2";

    public static void main(String[] args) throws IOException {
        log.info("Starting Ai model trainer for lego with {} bricks", numClasses);
        final long startTime = System.currentTimeMillis();

        ZooModel zooModel = VGG16.builder().build();
        ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained();
        log.info(vgg16.summary());

        //Decide on a fine tune configuration to use.
        //In cases where there already exists a setting the fine tune setting will
        //  override the setting for all layers that are not "frozen".
        FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder()
                .updater(new Nesterovs(5e-5))
                .seed(seed)
                .build();

        //Construct a new model with the intended architecture and print summary
        ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16)
                .fineTuneConfiguration(fineTuneConf)
                .setFeatureExtractor(featureExtractionLayer) //the specified layer and below are "frozen"
                .removeVertexKeepConnections("predictions") //replace the functionality of the final vertex
                .addLayer("predictions",
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                                .nIn(4096).nOut(numClasses)
                                .weightInit(new NormalDistribution(0, 0.2 * (2.0 / (4096 + numClasses)))) //This weight init dist gave better results than Xavier
                                .activation(Activation.SOFTMAX).build(),
                        "fc2")
                .build();
        log.info(vgg16Transfer.summary());

        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);

        //Then add the StatsListener to collect this information from the network, as it trains
        vgg16Transfer.setListeners(new StatsListener(statsStorage));

        log.info("Model setup complete");

        //Dataset iterators
        LegoDataSetIterator.setup(batchSize, trainPerc);
        log.info("Dataset loaded");
        DataSetIterator trainIter = LegoDataSetIterator.trainIterator();
        log.info("Train iter loaded");
        DataSetIterator testIter = LegoDataSetIterator.testIterator();
        log.info("Test iter loaded");

        exportLabels(trainIter);
        log.info("Labels exported");

        Evaluation eval;
        eval = vgg16Transfer.evaluate(testIter);
        log.info("Eval stats BEFORE fit.....");
        log.info(eval.stats() + "\n");
        testIter.reset();

        int totalIters = 0;
        while (trainIter.hasNext()) {
            trainIter.next();
            totalIters++;
        }
        trainIter.reset();
        log.info("Total train iters: " + totalIters);

        int iter = 0;
        while (trainIter.hasNext()) {
            log.info("Start train iter " + iter + "/" + totalIters + "....");
            vgg16Transfer.fit(trainIter.next());
            if (iter % 10 == 0) {
                log.info("Evaluate model at iter " + iter + "/" + totalIters + " ....  in " + (System.currentTimeMillis() - startTime) / 1000 + "sec");
                eval = vgg16Transfer.evaluate(testIter);
                log.info(eval.stats());
                testIter.reset();
            }
            iter++;
        }

        log.info("Model build complete in " + (System.currentTimeMillis() - startTime) / 1000 + "sec");
        vgg16Transfer.save(new File("lego_model.zip"));
    }

    private static void exportLabels(DataSetIterator trainIter) throws IOException {
        FileWriter writer = new FileWriter("lego_model_labels.csv");
        List<String> test = trainIter.getLabels();

        String collect = String.join(",", test);
        writer.write(collect);
        writer.close();
    }
}
