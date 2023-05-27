package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.model.ModelInstanceImpl;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface ModelInstance<T> {

    static <T> ModelInstance<T> of(ModelType<T> modelType, T modelData){
        return new ModelInstanceImpl<>(modelType, modelData);
    }

    ModelType<T> getModelType();

    T getModelData();

    /**
     * Gets all the dependencies on other model files.
     */
    Collection<ResourceLocation> getModelDependencies();

    /**
     * Gets all the dependencies on sprites.
     * @param context context for gathering texture dependencies
     */
    Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context);

    /**
     * Converts the model data into a baked model.
     * @param context context for baking the model
     * @return a baked model
     * @see ModelBakingContext
     */
    IBakedModel bake(ModelBakingContext context);

    /**
     * Represents the model as a vanilla {@link ModelBlock} instance. May be used gather info from other models, such as with the vanilla 'parent' property.
     * @return a representation of the model as a vanilla {@link ModelBlock} instance, or {@code null} if such a representation is not available
     */
    @Nullable
    ModelBlock getAsVanillaModel();
}
