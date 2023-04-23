package org.acme.lego.util;

import ai.djl.Model;
import ai.djl.basicmodelzoo.cv.classification.ResNetV1;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;

public class AiModelHelper {

    public static final String MODEL_NAME = "lego";

    public static final int height = 224;
    public static final int width = 224;
    public static final int channels = 3;

    public static Model getModel() {
        Model model = Model.newInstance(MODEL_NAME);

        Block resNet50 = ResNetV1.builder()
                .setImageShape(new Shape(channels, width, height))
                .setNumLayers(50)
                .setOutSize(100)
                .build();

        model.setBlock(resNet50);
        return model;
    }
}
