package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ModelType<T> extends Serializer<T> {

    /**
     * Gets all the dependencies on other model files.
     * @param data custom model data
     */
    Collection<ResourceLocation> getModelDependencies(T data);

    /**
     * Converts the model data into a baked model.
     * @param context context for baking the model
     * @param data    custom model data
     * @return a baked model
     * @see ModelBakingContext
     */
    BakedModel bake(ModelBakingContext context, T data);

    /**
     * Represents the model as a vanilla {@link BlockModel} instance. May be used gather info from other models, such as with the vanilla 'parent' property.
     * If the model cannot be represented as a {@link BlockModel} instance, this method should return {@code null}.
     * @param data custom model data
     * @return a representation of the model as a vanilla {@link BlockModel} instance, or {@code null} if such a representation is not available
     */
    @Nullable
    default BlockModel getAsVanillaModel(T data){
        return null;
    }
}
