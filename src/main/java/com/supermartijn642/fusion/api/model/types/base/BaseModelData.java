package com.supermartijn642.fusion.api.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataBuilderImpl;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Data for the base model type.
 * <p>
 * Created 06/09/2024 by SuperMartijn642
 * @see com.supermartijn642.fusion.api.model.DefaultModelTypes#BASE
 */
@ApiStatus.NonExtendable
public interface BaseModelData {

    /**
     * Creates a builder for base model data.
     */
    static Builder<?,BaseModelData> builder(){
        return BaseModelDataBuilderImpl.builder();
    }

    /**
     * Parent of the model.
     */
    @Nullable
    ResourceLocation getParent();

    /**
     * Material references of the model.
     */
    Map<String,Either<String,ModelMaterial>> getMaterials();

    /**
     * Geometry of the model.
     */
    @Nullable
    CuboidModelGeometry getGeometry();

    /**
     * Whether the model should be rendered with ambient occlusion.
     */
    @Nullable
    Boolean getAmbientOcclusion();

    /**
     * Lighting to use when the model is rendered in a gui.
     */
    @Nullable
    UnbakedModel.GuiLight getGuiLight();

    /**
     * Transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransform getItemTransform(ItemDisplayContext type);

    /**
     * Whether the model should be shaded.
     */
    @Nullable
    Boolean getShade();

    /**
     * Whether the model is emissive.
     */
    @Nullable
    Boolean getEmissive();

    @ApiStatus.NonExtendable
    interface Builder<T extends Builder<T,S>, S> extends CuboidModelDataBuilder<T,S> {

        /**
         * Sets whether the model should be shaded.
         */
        T shade(@Nullable Boolean shade);

        /**
         * Sets whether the model is emissive.
         */
        T emissive(@Nullable Boolean emissive);
    }
}
