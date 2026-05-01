package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ModelType<T> extends Serializer<T> {

    /**
     * Gets all the dependencies on other model files.
     * @param data custom model data
     */
    Collection<Identifier> getModelDependencies(T data);

    /**
     * Converts the model data into a baked model.
     * @param context context for baking the model
     * @param data    custom model data
     * @return a baked model
     * @see BlockModelBakingContext
     */
    BlockStateModel bakeBlockModel(BlockModelBakingContext context, T data);

    ItemModel bakeItemModel(ItemModelBakingContext context, T data);

    /**
     * Represents the model as a vanilla {@link UnbakedModel} instance. May be used gather info from other models, such as with the vanilla 'parent' property.
     * If the model cannot be represented as a {@link UnbakedModel} instance, this method should return {@code null}.
     * @param data custom model data
     * @return a representation of the model as a vanilla {@link UnbakedModel} instance, or {@code null} if such a representation is not available
     */
    @Nullable
    UnbakedModel getAsVanillaModel(T data);

    /**
     * Gets any 'parent' models which the model may inherit properties from.
     */
    List<Identifier> getParentModels(T data);
}
