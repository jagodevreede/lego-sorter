package org.acme.lego.train;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicdataset.cv.classification.ImageFolder;
import ai.djl.engine.Engine;
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.SymbolBlock;
import ai.djl.nn.core.Linear;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.util.ProgressBar;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.acme.lego.preprocessing.ImagePreProcessor.POVRAY_CROPPED;
import static org.acme.lego.util.AiModelHelper.*;

@Slf4j
public class Learning {

    private static final AtomicInteger epochs = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        log.info("Starting to learn");
        ImageFolder dataset = loadDataset(POVRAY_CROPPED);
        RandomAccessDataset[] datasets = dataset.randomSplit(80, 20);

        Engine.getInstance().setRandomSeed(42);

        try (Model model = getModel();
             Trainer trainer = model.newTrainer(getTrainingConfig())) {
            Path modelDir = Paths.get("model");
            trainer.setMetrics(new Metrics());

            trainer.initialize(new Shape(1, channels, width, height));

            final long startTime = System.currentTimeMillis();

            EarlyStoppingFit earlyStoppingFit = getEarlyStoppingFit(model, modelDir);
            earlyStoppingFit.fit(trainer, datasets[0], datasets[1]);

            log.info("Model build complete in " + (System.currentTimeMillis() - startTime) / 1000 + "sec");

            // set model properties
            TrainingResult result = trainer.getTrainingResult();
            model.setProperty("Epoch", String.valueOf(epochs.get()));
            model.setProperty("Accuracy", String.format("%.5f", result.getValidateEvaluation("Accuracy")));
            model.setProperty("Loss", String.format("%.5f", result.getValidateLoss()));

            model.save(modelDir, "lego");
            saveLabels(modelDir, dataset.getSynset());

            log.info("Total build complete in " + (System.currentTimeMillis() - startTime) / 1000 + "sec");

            printStatisticsPerEpoch(trainer);
        }
    }

    private static EarlyStoppingFit getEarlyStoppingFit(Model model, Path modelDir) {
        AtomicReference<Double> bestValidationLoss = new AtomicReference<>(99999.9);
        EarlyStoppingFit earlyStoppingFit =
                new EarlyStoppingFit(10, 0.02, 10,
                        9 * 60, 1, 5);
        earlyStoppingFit.addCallback((m, epoch, validationLoss) -> {
            epochs.incrementAndGet();
            try {
                String filename = "lego-e-" + epoch + "-v-" + validationLoss;
                if (validationLoss < bestValidationLoss.get()) {
                    model.save(modelDir, filename + "-best");
                    bestValidationLoss.set(validationLoss);
                } else {
                    model.save(modelDir, filename);
                }
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            }
        });
        return earlyStoppingFit;
    }

    private static void printStatisticsPerEpoch(Trainer trainer) {
        Metrics metrics = trainer.getMetrics();
        Map<String, double[]> evaluatorMetrics = new HashMap<>();
        trainer.getEvaluators()
                .forEach(evaluator -> {
                    evaluatorMetrics.put("train_epoch_" + evaluator.getName(), metrics.getMetric("train_epoch_" + evaluator.getName()).stream()
                            .mapToDouble(Metric::getValue).toArray());
                    evaluatorMetrics.put("validate_epoch_" + evaluator.getName(), metrics.getMetric("validate_epoch_" + evaluator.getName()).stream()
                            .mapToDouble(Metric::getValue).toArray());
                });

        double[] trainLoss = evaluatorMetrics.get("train_epoch_SoftmaxCrossEntropyLoss");
        double[] testLoss = evaluatorMetrics.get("validate_epoch_SoftmaxCrossEntropyLoss");
        double[] trainAccuracy = evaluatorMetrics.get("train_epoch_Accuracy");
        double[] testAccuracy = evaluatorMetrics.get("validate_epoch_Accuracy");

        log.info("epoch,train loss,test loss,train accuracy,test accuracy");
        for (int i = 0; i < trainAccuracy.length; i++) {
            log.info(i + 1 + "," + trainLoss[i] + "," + testLoss[i] + "," + trainAccuracy[i] + "," + testAccuracy[i]);
        }
        // Force exit, something keeps running in the background, TODO figure out why app is not exiting.
        System.exit(0);
    }

    private static void saveLabels(Path modelDir, List<String> synset) throws IOException {
        final Path labelFile = modelDir.resolve("synset.txt");
        try (Writer writer = Files.newBufferedWriter(labelFile)) {
            writer.write(String.join("\n", synset));
        }
    }

    private static TrainingConfig getTrainingConfig() {
        return new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .optOptimizer(Optimizer.adam().build())
                .addEvaluator(new Accuracy())
                .addTrainingListeners(TrainingListener.Defaults.logging(1));
    }

    private static Model getTransferLearningModel() throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria.Builder<Image, Classifications> builder = getModelBuilder();
        Model model = builder.build().loadModel();
        SymbolBlock block = (SymbolBlock) model.getBlock();
        block.removeLastBlock();
        // freeze original model
        block.freezeParameters(true);

        SequentialBlock newBlock = new SequentialBlock();
        newBlock.add(block);
        // the original model don't include the flatten so apply the flatten here
        newBlock.add(Blocks.batchFlattenBlock());
        newBlock.add(Linear.builder().setUnits(2).build());
        model.setBlock(newBlock);
        return model;
    }

    private static Criteria.Builder<Image, Classifications> getModelBuilder() {
        Criteria.Builder<Image, Classifications> builder =
                Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(Image.class, Classifications.class)
                        .optProgress(new ProgressBar())
                        .optArtifactId("resnet");
        builder.optGroupId("ai.djl.mxnet");
        builder.optFilter("layers", "50");
        builder.optFilter("flavor", "v1");
        return builder;
    }

    private static ImageFolder loadDataset(String folder) throws IOException {
        ImageFolder dataset = ImageFolder.builder()
                .setRepositoryPath(Paths.get(folder))
//                .addTransform(new TestTransform())
                .addTransform(new Resize(224, 224))
               // .addTransform(new RandomColorJitter(0.4f, 0.4f, 0.4f, 0.4f)) // not supported on GPU
//                .addTransform(new RandomFlipLeftRight())
//                .addTransform(new RandomFlipTopBottom())
                .addTransform(new ToTensor())
                .setSampling(32, true)
                .optFlag(Image.Flag.GRAYSCALE)
                .build();
        dataset.prepare(new ProgressBar());
        return dataset;
    }
}
