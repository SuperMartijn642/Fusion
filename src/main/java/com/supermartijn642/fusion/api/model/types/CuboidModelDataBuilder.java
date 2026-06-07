package com.supermartijn642.fusion.api.model.types;

import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.cuboid.CuboidModelDataBuilderImpl;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for vanilla cuboid model data properties.
 * <p>
 * Created 01/05/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface CuboidModelDataBuilder<T extends CuboidModelDataBuilder<T,S>, S> {

    /**
     * Creates a builder for a vanilla cuboid model.
     */
    static CuboidModelDataBuilder<?,BlockModel> builder(){
        return CuboidModelDataBuilderImpl.builder();
    }

    /**
     * Sets the parent model.
     */
    T parent(@Nullable ResourceLocation parent);

    /**
     * Sets the given value for the given materials key.
     */
    T material(String key, Either<String,ModelMaterial> material);

    /**
     * Sets the given materials key to redirect to the given reference.
     */
    default T material(String key, String reference){
        return this.material(key, Either.left(reference));
    }

    /**
     * Sets the material for the given key.
     */
    default T material(String key, ResourceLocation texture){
        return this.material(key, texture);
    }

    /**
     * Adds the given elements to the geometry.
     */
    T elements(CuboidModelGeometry.Element... elements);

    /**
     * Sets the lighting to use when the model is rendered in a gui.
     */
    T guiLight(@Nullable BlockModel.GuiLight guiLight);

    /**
     * Sets whether the model should be rendered with ambient occlusion.
     */
    T ambientOcclusion(@Nullable Boolean ambientOcclusion);

    /**
     * Sets the transformations used to render the model as an item under the given context.
     */
    T itemTransform(ItemDisplayContext type, @Nullable ItemTransform transform);

    /**
     * Sets the transformations used to render the model as an item.
     */
    T itemTransforms(ItemTransforms itemTransforms);

    /**
     * @see DefaultModelProperties#NEO_MODEL_RENDER_TYPE
     */
    T neoRenderTypeGroup(@Nullable RenderTypeGroup renderTypeGroup);

    /**
     * Builds the model data.
     */
    S build();
}
