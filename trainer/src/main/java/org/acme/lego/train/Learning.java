package org.acme.lego.train;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicdataset.cv.classification.ImageFolder;
import ai.djl.basicmodelzoo.cv.classification.ResNetV1;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.SymbolBlock;
import ai.djl.nn.core.Linear;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.acme.lego.preprocessing.ImagePreProcessor.POVRAY_CROPPED;
import static org.acme.lego.util.AiModelHelper.*;

@Slf4j
public class Learning {

    public static void main(String[] args) throws Exception {
        log.info("Starting to learn");
        ImageFolder dataset = loadDataset(POVRAY_CROPPED);
        RandomAccessDataset[] datasets = dataset.randomSplit(80, 20);

        try (Model model = getModel();
             Trainer trainer = model.newTrainer(getTrainingConfig())) {
            Path modelDir = Paths.get("model");
            trainer.setMetrics(new Metrics());

            trainer.initialize(new Shape(1, channels, width, height));

            AtomicReference<Double> bestValidationLoss = new AtomicReference<>(99999.9);
            EarlyStoppingFit earlyStoppingFit =
                    new EarlyStoppingFit(20, 0.2, 3,
                            9 * 60, 0.1, 3);
            earlyStoppingFit.addCallback((m, epoch, validationLoss) -> {
                try {
                    if (validationLoss < bestValidationLoss.get()) {
                        model.save(modelDir, "lego-e-" + epoch + "-v-" + validationLoss);
                        bestValidationLoss.set(validationLoss);
                    }
                } catch (final IOException ioe) {
                    ioe.printStackTrace();
                }
            });
            earlyStoppingFit.fit(trainer, datasets[0], datasets[1]);

            model.save(modelDir, "lego");
            saveLabels(modelDir, dataset.getSynset());
        }
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

    private static Model getModel() {
        Model model = Model.newInstance("lego");

        Block resNet50 = ResNetV1.builder()
                .setImageShape(new Shape(3, 224, 224))
                .setNumLayers(50)
                .setOutSize(2)
                .build();

        model.setBlock(resNet50);
        return model;
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
                .addTransform(new Resize(224, 224))
                .addTransform(new ToTensor())
                .setSampling(8, true)
                .build();
        dataset.prepare(new ProgressBar());
        return dataset;
    }
}
