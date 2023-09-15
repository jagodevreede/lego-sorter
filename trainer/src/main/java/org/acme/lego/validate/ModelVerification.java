package org.acme.lego.validate;

import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.acme.lego.util.AiModelHelper.MODEL_NAME;
import static org.acme.lego.util.AiModelHelper.getModel;


public class ModelVerification {
    private static final Logger logger = LoggerFactory.getLogger(ModelVerification.class);

    private static final String VALIDATION_SET = "samples_mobile/cropped";

    private static Path modelDir = Paths.get("model");

    private static Path multipleModelsDir = Paths.get("models");

    private final Map<String, Map<String, Integer>> matrix = new HashMap<>();

    private final static Map<String, Double> totalScore = new HashMap<>();

    private double accuracy;

    public static void main(String[] args) throws Exception {
        File targetSynset = new File(modelDir.toFile(), "synset.txt");
        for (File file : multipleModelsDir.toFile().listFiles((dir, name) -> name.endsWith(".params"))) {
            logger.info("Testing model {}", file.getName());
            // copy file to moodel dir
            File targetFile = new File(modelDir.toFile(), "lego-0000.params");

            targetFile.delete();
            targetSynset.delete();
            Path fileToCopy = Paths.get(file.getAbsolutePath());
            try {
                Files.copy(fileToCopy, targetFile.toPath());
                Files.copy(new File(multipleModelsDir.toFile(), "synset.txt").toPath(), targetSynset.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (Model model = getModel()) {
                model.load(modelDir, MODEL_NAME);
                long startTime = System.currentTimeMillis();
                totalScore.put(file.getName(), new ModelVerification(model).accuracy);
                logger.info("Validation done in {}ms", System.currentTimeMillis() - startTime);
            }
        }
        totalScore.keySet().stream().sorted().forEach(key -> {
            logger.info("Model {} accuracy {}", key, totalScore.get(key));
        });
    }

    private static Translator<Image, Classifications> createTranslator() {
        return ImageClassificationTranslator.builder()
//                .addTransform(new Resize(width, height))
//                .addTransform(new TestTransform())
                .addTransform(new ToTensor())
                .optFlag(Image.Flag.GRAYSCALE)
                .optApplySoftmax(true)
                .build();
    }

    public ModelVerification(Model model) {
        // define a translator for pre and post processing
        // out of the box this translator converts images to ResNet friendly ResNet 18 shape
        Translator<Image, Classifications> translator = createTranslator();
        // run the inference using a Predictor
        try (Predictor<Image, Classifications> predictor = model.newPredictor(translator)) {
            Stream.of(new File(VALIDATION_SET).listFiles(filter -> filter.isDirectory())).forEach(folder -> {
                Stream.of(folder.listFiles(filter -> filter.isFile())).forEach(file -> {
                    predictFile(predictor, folder, file);
                });
            });
        }
        printConfusionMatrix();
    }

    public double getAccuracy() {
        return accuracy;
    }

    private void predictFile(Predictor<Image, Classifications> predictor, File folder, File file) {
        try {
            Path imageFile = file.toPath();
            Image img = ImageFactory.getInstance().fromFile(imageFile);
            final long startTime = System.currentTimeMillis();
            // holds the probability score per label
            Classifications predictResult = predictor.predict(img);
            if (predictResult.items().isEmpty()) {
                logger.error("No prediction for " + imageFile.toString());
                return;
            }
            final long endTime = System.currentTimeMillis();

            List<Classifications.Classification> classificationList = predictResult.topK(1);
            Classifications.Classification classification = classificationList.get(0);
            String className = classification.getClassName();
            String classNameToLog = classification.getClassName();
            if (classification.getProbability() > 0.8) {
                var outcome = matrix.getOrDefault(folder.getName(), new HashMap<>());
                var count = outcome.getOrDefault(className, 0);
                outcome.put(className, count + 1);
                matrix.put(folder.getName(), outcome);
            } else {
                var outcome = matrix.getOrDefault(folder.getName(), new HashMap<>());
                var count = outcome.getOrDefault("unsure " + className, 0);
                outcome.put("unsure " + className, count + 1);
                matrix.put(folder.getName(), outcome);
                classNameToLog = "unsure " + className + "?";
            }

            if (!folder.getName().equals(className)) {
                logger.debug("(Incorrect) Prediction took total {}ms {} with probability {}", endTime - startTime, classNameToLog, classification.getProbability());
            } else {
                logger.debug(" (correct)  Prediction took total {}ms {} with probability {}", endTime - startTime, classNameToLog, classification.getProbability());
            }
        } catch (TranslateException | IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void printConfusionMatrix() {
        final Set<String> allKeys = new HashSet<>(matrix.keySet().stream().toList());
        allKeys.addAll(matrix.values().stream().flatMap(m -> m.keySet().stream()).toList());

        // Find the length of the longest key in the outer map and the inner maps
        int maxKeyLength = allKeys.stream().mapToInt(String::length).max().orElse(0) + 1;

        // Calculate the diagonal sum and the total sum
        int diagonalSum = 0;
        int totalSum = 0;
        for (String actual : allKeys) {
            Map<String, Integer> innerMap = matrix.get(actual);
            if (innerMap == null) {
                continue;
            }
            for (String predicted : matrix.keySet()) {
                int count = innerMap.getOrDefault(predicted, 0);
                totalSum += count;
                if (actual.equals(predicted)) {
                    diagonalSum += count;
                }
            }
        }

        // Print header row
        System.out.printf("%-" + maxKeyLength + "s", "");
        for (String actual : allKeys) {
            System.out.printf("%-" + (maxKeyLength + 1) + "s", actual);
        }
        System.out.printf("%-13s%n", "Accuracy");

        // Print rows
        for (String actual : matrix.keySet()) {
            System.out.printf("%-" + maxKeyLength + "s", actual);
            Map<String, Integer> innerMap = matrix.get(actual);
            int rowSum = 0;
            int correctCount = innerMap.getOrDefault(actual, 0);
            for (String predicted : allKeys) {
                int count = innerMap.getOrDefault(predicted, 0);
                rowSum += count;
                System.out.printf("%-" + (maxKeyLength + 1) + "d", count);
            }
            double accuracy = (double) correctCount / rowSum;
            System.out.printf("%-13.2f%n", accuracy * 100);
        }

        accuracy = (double) diagonalSum / totalSum;
        System.out.printf("Total accuracy: %-13.2f%n", accuracy * 100);
    }
}
