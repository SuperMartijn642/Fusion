package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelProperty;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.ModelInstanceImpl;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A container for a model type along with its data.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelInstance<T> {

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
     * Gets the lighting to use when the model is rendered in a gui.
     */
    @Nullable
    BlockModel.GuiLight getGuiLight();

    /**
     * Gets the transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransform getItemTransform(ItemTransforms.TransformType type);

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
     * Gets an arbitrary property of this model.
     * @see ModelProperty
     */
    <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context);

    /**
     * Gets an arbitrary property of this model.
     * @see ModelProperty
     */
    default <X> Optional<X> getProperty(ModelProperty<X,Void> property){
        return this.getProperty(property, null);
    }

    /**
     * Creates a baked model from the model data.
     */
    BakedModel bakeModel(ModelBakingContext context);
}
