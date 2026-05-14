package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An interface for custom model types.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 * @param <T> data used to bake the model
 */
public interface ModelType<T> extends Serializer<T> {

    /**
     * Gets all the dependencies on other unbaked models.
     */
    Collection<ResourceLocation> getDependencies(T data);

    /**
     * Gets any parent models which the model may inherit properties from.
     */
    List<Either<ResourceLocation,ModelInstance<?>>> getParents(T data);

    /**
     * Gets whether the model should be rendered with ambient occlusion.
     */
    @Nullable
    Boolean getAmbientOcclusion(T data);

    /**
     * Gets whether the model is flat or 3d.
     */
    @Nullable
    Boolean getIsGui3d(T data);

    /**
     * Gets the transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, T data);

    /**
     * Gets the material references of the model.
     */
    Map<String,Either<String,ModelMaterial>> getMaterials(T data);

    /**
     * Gets the geometry of the model.
     */
    @Nullable
    ModelGeometry getGeometry(T data);

    /**
     * Gets whether the model should be shaded.
     */
    @Nullable
    Boolean getShade(T data);

    /**
     * Gets whether the model is emissive.
     */
    @Nullable
    Boolean getEmissive(T data);

    /**
     * Gets the transformations that should be applied to the model's geometry.
     */
    default ModelTransform getTransform(T data){
        return ModelTransform.identity();
    }

    /**
     * Gets an arbitrary property of this model.
     * @see Property
     */
    <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data);

    /**
     * Creates a baked model from the model data.
     * @see ModelBakingContext
     */
    IBakedModel bakeModel(ModelBakingContext context, T data);
}
