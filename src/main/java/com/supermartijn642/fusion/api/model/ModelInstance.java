package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.ModelInstanceImpl;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A container for a model type along with its data.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelInstance<T> extends PropertyGetter {

    /**
     * Create a new model instance for the given values.
     */
    static <T> ModelInstance<T> of(ModelType<T> modelType, T modelData){
        return new ModelInstanceImpl<>(modelType, modelData);
    }

    /**
     * The type of the model.
     */
    ModelType<T> getModelType();

    /**
     * The data of the model.
     */
    T getModelData();

    /**
     * Gets all the dependencies on other unbaked models.
     */
    Collection<ResourceLocation> getDependencies();

    /**
     * Gets any parent models which the model may inherit properties from.
     */
    List<Either<ResourceLocation,ModelInstance<?>>> getParents();

    /**
     * Gets whether the model should be rendered with ambient occlusion.
     */
    @Nullable
    Boolean getAmbientOcclusion();

    /**
     * Gets whether the model is flat or 3d.
     */
    @Nullable
    Boolean getIsGui3d();

    /**
     * Gets the transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type);

    /**
     * Gets the material references of the model.
     */
    Map<String,Either<String,ModelMaterial>> getMaterials();

    /**
     * Gets the material reference for the given key.
     */
    @Nullable
    default Either<String,ModelMaterial> getMaterial(String key){
        return this.getMaterials().get(key);
    }

    /**
     * Gets the geometry of the model.
     */
    @Nullable
    ModelGeometry getGeometry();

    /**
     * Gets whether the model should be shaded.
     */
    @Nullable
    Boolean getShade();

    /**
     * Gets whether the model is emissive.
     */
    @Nullable
    Boolean getEmissive();

    /**
     * Gets the transformations that should be applied to the model's geometry.
     */
    ModelTransform getTransform();

    /**
     * Creates a baked model from the model data.
     */
    IBakedModel bakeModel(ModelBakingContext context);
}
