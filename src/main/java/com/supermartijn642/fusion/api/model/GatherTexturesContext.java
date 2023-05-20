package com.supermartijn642.fusion.api.model;

import net.minecraft.resources.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface GatherTexturesContext {

    /**
     * Gets the model for the given identifier.
     * @return a pair containing the model type and the model's data
     * @throws IllegalArgumentException when the requested model was not in {@link ModelType#getModelDependencies(Object)}
     */
    ModelInstance<?> getModel(ResourceLocation identifier);
}
