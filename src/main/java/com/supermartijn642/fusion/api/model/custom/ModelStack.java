package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.custom.ModelStackImpl;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A stack of models representing a branch of a model tree.
 * The first element in the stack represents the top of tree, with each consecutive element being a parent of the one before it.
 * <p>
 * Created 06/06/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelStack extends Iterable<UntypedModelInstance> {

    static ModelStack empty(){
        return ModelStackImpl.empty();
    }

    /**
     * Creates a copy of the stack with the given model appended.
     * @param instance   model to append
     * @param identifier identifier of the model, may be {@code null} for unnamed models
     */
    ModelStack push(UntypedModelInstance instance, @Nullable ResourceLocation identifier);

    /**
     * Creates a copy of the stack with the given model appended.
     */
    default ModelStack push(UntypedModelInstance instance){
        return this.push(instance, null);
    }

    /**
     * Size of the model stack.
     */
    int size();

    /**
     * Element at the given index.
     * The first element in the stack represents the top of tree, with each consecutive element being a parent of the one before it.
     */
    UntypedModelInstance get(int index);

    /**
     * Identifier of the model at the given index, may be {@code null} for unnamed models.
     */
    @Nullable
    ResourceLocation getIdentifier(int index);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getAmbientOcclusion()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findAmbientOcclusion();

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getAmbientOcclusion()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findAmbientOcclusionIncludingParents(ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getGuiLight()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    BlockModel.GuiLight findGuiLight();

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getGuiLight()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    BlockModel.GuiLight findGuiLightIncludingParents(ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getItemTransform(ItemCameraTransforms.TransformType)} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    ItemTransformVec3f findItemTransform(ItemCameraTransforms.TransformType type);

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getItemTransform(ItemCameraTransforms.TransformType)} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    ItemTransformVec3f findItemTransformIncludingParents(ItemCameraTransforms.TransformType type, ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getMaterial(String)} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Either<String,ModelMaterial> findMaterial(String key);

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getMaterial(String)} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Either<String,ModelMaterial> findMaterialIncludingParents(String key, ModelResolver modelResolver);

    /**
     * Finds the material references for the current model stack.
     * A key's value is that of the first model in the stack that defined it.
     */
    Map<String,Either<String,ModelMaterial>> findMaterials();

    /**
     * Finds the material references for the current model stack and the model tree of the last model.
     * A key's value is that of the first model in the stack that defined it.
     */
    Map<String,Either<String,ModelMaterial>> findMaterialsIncludingParents(ModelResolver modelResolver);

    /**
     * Resolves a material key, by recursively finding references for the key until a material is found. If a key cannot be resolved, the result is {@code null}.
     * @param key            material key to resolve
     * @param reportCircular consumer for reporting circular material references, the chain of references is given as an argument
     */
    @Nullable
    ModelMaterial findMaterialRecursive(String key, Consumer<List<String>> reportCircular);

    /**
     * Resolves a material key, by recursively finding references for the key until a material is found. If a key cannot be resolved, the result is {@code null}.
     * @param key            material key to resolve
     * @param reportCircular consumer for reporting circular material references, the chain of references is given as an argument
     */
    @Nullable
    ModelMaterial findMaterialRecursiveIncludingParents(String key, Consumer<List<String>> reportCircular, ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getGeometry()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    ModelGeometry findGeometry();

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getGeometry()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    ModelGeometry findGeometryIncludingParents(ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getShade()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findShade();

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getShade()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findShadeIncludingParents(ModelResolver modelResolver);

    /**
     * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getEmissive()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findEmissive();

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a non-null value for {@link ModelInstance#getEmissive()} and returns its value.
     * If no such model is present, the result is {@code null}.
     */
    @Nullable
    Boolean findEmissiveIncludingParents(ModelResolver modelResolver);

    /**
     * Composes the transformations from models in the stack.
     * @see ModelInstance#getTransform()
     */
    ModelTransform composeTransforms();

    /**
     * Combines conditions from the models in the stack.
     * If no model has a condition, the result is {@code null}.
     */
    @Nullable
    ModelPredicate combineConditions();

    /**
     * Finds the first model in the stack that returns a value for {@link ModelInstance#getProperty(Property, Object)} and returns its value.
     * If no such model is present, the result is {@link Optional#empty()}.
     */
    <X, C> Optional<X> findProperty(Property<X,C> property, C context);

    /**
     * Finds the first model in the stack that returns a value for {@link ModelInstance#getProperty(Property)} and returns its value.
     * If no such model is present, the result is {@link Optional#empty()}.
     */
    default <X> Optional<X> findProperty(Property<X,Void> property){
        return this.findProperty(property, null);
    }

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a value for {@link ModelInstance#getProperty(Property, Object)} and returns its value.
     * If no such model is present, the result is {@link Optional#empty()}.
     */
    <X, C> Optional<X> findPropertyIncludingParents(Property<X,C> property, C context, ModelResolver modelResolver);

    /**
     * Finds the first model in the stack and the model tree of the last model that returns a value for {@link ModelInstance#getProperty(Property)} and returns its value.
     * If no such model is present, the result is {@link Optional#empty()}.
     */
    default <X> Optional<X> findPropertyIncludingParents(Property<X,Void> property, ModelResolver modelResolver){
        return this.findProperty(property, null);
    }

    /**
     * Walks the models in the stack.
     * @param walker consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    <T> Optional<T> walkStack(ModelWalker<T> walker);

    /**
     * Walks the models in the stack and the model tree of last model in the stack.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param modelResolver resolver for parents defined by identifier
     * @param walker        consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    <T> Optional<T> walkStackAndParents(ModelResolver modelResolver, ModelWalker<T> walker);

    /**
     * Walks the model tree of the last model in the stack.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param modelResolver resolver for parents defined by identifier
     * @param walker        consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    <T> Optional<T> walkParents(ModelResolver modelResolver, ModelWalker<T> walker);
}
