package com.supermartijn642.fusion.api.model;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import net.minecraft.util.ResourceLocation;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public final class FusionModelTypeRegistry {

    /**
     * Registers the given model type.
     * @param identifier identifier for the model type
     * @param modelType  handler for the custom model
     */
    public static void registerModelType(ResourceLocation identifier, ModelType<?> modelType){
        ModelTypeRegistryImpl.registerModelType(identifier, modelType);
    }

    /**
     * Serializes the given model.
     * @param model model to be serialized
     */
    public static JsonObject serializeModelData(ModelInstance<?> model){
        return ModelTypeRegistryImpl.serializeModelData(model);
    }
}
