package com.supermartijn642.fusion.api.model.types.composite;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.item.DefaultItemModelPredicates;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.composite.CompositeModelDataBuilderImpl;
import com.supermartijn642.fusion.model.types.composite.CompositeModelDataImpl;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface CompositeModelData {

    /**
     * Creates a builder for composite model data.
     */
    static Builder builder(){
        return CompositeModelDataBuilderImpl.builder();
    }

    /**
     * Series of models, where for each series, the first model whose condition is met is applied
     */
    List<List<ModelEntry>> getModels();

    /**
     * Material references of the model.
     */
    Map<String,Either<String,ModelMaterial>> getMaterials();

    /**
     * Whether the model should be rendered with ambient occlusion.
     */
    @Nullable
    Boolean getAmbientOcclusion();

    /**
     * Whether the model is flat or 3d.
     */
    @Nullable
    Boolean getIsGui3d();

    /**
     * Transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type);

    /**
     * Model overwrites dependent on item stack properties.
     */
    List<ItemOverride> getItemOverrides();

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
    interface ModelEntry {

        /**
         * @param model     location of the model
         * @param transform transform to apply to the model
         * @param condition condition for the model entry
         * @see ModelTransform#identity()
         * @see DefaultModelPredicates
         * @see DefaultBlockStateModelPredicates
         * @see DefaultItemModelPredicates
         */
        static ModelEntry of(ResourceLocation model, ModelTransform transform, @Nullable ModelPredicate condition){
            return CompositeModelDataImpl.entry(model, transform, condition);
        }

        /**
         * @param model     model to apply
         * @param transform transform to apply to the model
         * @param condition condition for the model entry
         * @see ModelTransform#identity()
         * @see DefaultModelPredicates
         * @see DefaultBlockStateModelPredicates
         * @see DefaultItemModelPredicates
         */
        static ModelEntry of(ModelInstance<?> model, ModelTransform transform, @Nullable ModelPredicate condition){
            return CompositeModelDataImpl.entry(model, transform, condition);
        }

        /**
         * Model to apply, either model location or a model instance.
         */
        Either<ResourceLocation,ModelInstance<?>> getModel();

        /**
         * Transform to apply to the model.
         */
        ModelTransform getTransform();

        /**
         * Condition for the model entry.
         */
        @Nullable
        ModelPredicate getCondition();
    }

    @ApiStatus.NonExtendable
    interface Builder {

        /**
         * Adds a model.
         */
        Builder model(ModelEntry entry);

        /**
         * Adds a model series. The first model in the series whose condition is met is applied.
         */
        Builder modelSeries(ModelEntry... entries);

        /**
         * Adds a model series. The first model in the series whose condition is met is applied.
         */
        Builder modelSeries(List<ModelEntry> entries);

        /**
         * Sets the given value for the given materials key.
         */
        Builder material(String key, Either<String,ModelMaterial> material);

        /**
         * Sets the given materials key to redirect to the given reference.
         */
        default Builder material(String key, String reference){
            return this.material(key, Either.left(reference));
        }

        /**
         * Sets the material for the given key.
         */
        default Builder material(String key, ResourceLocation texture){
            return this.material(key, Either.right(ModelMaterial.of(texture)));
        }

        /**
         * Sets whether the model is flat or 3d.
         */
        Builder isGui3d(@Nullable Boolean isGui3d);

        /**
         * Sets whether the model should be rendered with ambient occlusion.
         */
        Builder ambientOcclusion(@Nullable Boolean ambientOcclusion);

        /**
         * Sets the transformations used to render the model as an item under the given context.
         */
        Builder itemTransform(ItemCameraTransforms.TransformType type, @Nullable ItemTransformVec3f transform);

        /**
         * Sets the transformations used to render the model as an item.
         */
        Builder itemTransforms(ItemCameraTransforms itemTransforms);

        /**
         * Adds the given item overwrites.
         */
        Builder itemOverrides(ItemOverride... overrides);

        /**
         * Sets whether the model should be shaded.
         */
        Builder shade(@Nullable Boolean shade);

        /**
         * Sets whether the model is emissive.
         */
        Builder emissive(@Nullable Boolean emissive);

        /**
         * Builds the model data.
         */
        CompositeModelData build();
    }
}
