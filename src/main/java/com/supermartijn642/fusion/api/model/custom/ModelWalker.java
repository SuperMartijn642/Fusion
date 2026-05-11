package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.custom.ModelWalkerImpl;
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
import java.util.function.Function;

/**
 * Utility class to walk a model tree.
 * From a starting model, model trees are explored depth-first by recursively jumping to parent models.
 * <p>
 * Created 04/05/2026 by SuperMartijn642
 */
@FunctionalInterface
public interface ModelWalker<T> {

    /**
     * Walks the model tree of the given model.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param modelResolver resolver for parents defined by identifier
     * @param modelInstance starting model, i.e. the model at the head of the tree
     * @param walker        consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    static <T> Optional<T> walkModelTree(Function<ResourceLocation,ModelInstance<?>> modelResolver, ModelInstance<?> modelInstance, ModelWalker<T> walker){
        return ModelWalkerImpl.walkModelTree(modelResolver, modelInstance, walker);
    }

    /**
     * Walks the model tree of the given model.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param modelResolver resolver for parents defined by identifier
     * @param model         identifier of the starting model, i.e. the model at the head of the tree
     * @param walker        consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    static <T> Optional<T> walkModelTree(Function<ResourceLocation,ModelInstance<?>> modelResolver, ResourceLocation model, ModelWalker<T> walker){
        return ModelWalkerImpl.walkModelTree(modelResolver, model, walker);
    }

    /**
     * Consumes one model in the model tree.
     * @param modelInstance the model instance
     * @param stack         the current stack of models
     * @return {@link Result#proceed()} when the walker should continue, {@link Result#endBranch()} when the walker should end exploring the current branch and continue with the next branch, and {@link Result#stop(Object)} when the walker should stop
     * @see Result
     */
    Result<T> consume(ModelInstance<?> modelInstance, ModelStack stack);

    @ApiStatus.NonExtendable
    interface Result<T> {

        /**
         * The walker should proceed with the next model in the branch.
         */
        static <T> Result<T> proceed(){
            return ModelWalkerImpl.proceed();
        }

        /**
         * The walker should end exploring the current branch and continue with the next branch.
         */
        static <T> Result<T> endBranch(){
            return ModelWalkerImpl.endBranch();
        }

        /**
         * The walker should stop and the given value should be returned.
         */
        static <T> Result<T> stop(T value){
            return ModelWalkerImpl.stop(value);
        }

        /**
         * The walker should stop.
         */
        static Result<Void> stop(){
            return ModelWalkerImpl.stop();
        }
    }

    /**
     * A stack of models representing the current branch of the model tree.
     * The first element in the stack represents the top of tree, with each consecutive element being a parent of the one before it.
     */
    @ApiStatus.NonExtendable
    interface ModelStack extends Iterable<ModelInstance<?>> {

        /**
         * Size of the model stack.
         */
        int size();

        /**
         * Element at the given index.
         * The first element in the stack represents the top of tree, with each consecutive element being a parent of the one before it.
         */
        ModelInstance<?> get(int index);

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
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getGuiLight()} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        BlockModel.GuiLight findGuiLight();

        /**
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getItemTransform(ItemCameraTransforms.TransformType)} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        ItemTransformVec3f findItemTransform(ItemCameraTransforms.TransformType type);

        /**
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getMaterial(String)} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        Either<String,ModelMaterial> findMaterial(String key);

        /**
         * Finds the material references for the current model stack.
         * A key's value is that of the first model in the stack that defined it.
         */
        Map<String,Either<String,ModelMaterial>> findMaterials();

        /**
         * Resolves a material key, by recursively finding references for the key until a material is found. If a key cannot be resolved, the result is {@code null}.
         * @param key            material key to resolve
         * @param reportCircular consumer for reporting circular material references, the chain of references is given as an argument
         */
        @Nullable
        ModelMaterial findMaterialRecursive(String key, Consumer<List<String>> reportCircular);

        /**
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getGeometry()} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        ModelGeometry findGeometry();

        /**
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getShade()} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        Boolean findShade();

        /**
         * Finds the first model in the stack that returns a non-null value for {@link ModelInstance#getEmissive()} and returns its value.
         * If no such model is present, the result is {@code null}.
         */
        @Nullable
        Boolean findEmissive();

        /**
         * Composes the transformations from models in the stack.
         * @see ModelInstance#getTransform()
         */
        ModelTransform composeTransforms();

        /**
         * Finds the first model in the stack that returns a value for {@link ModelInstance#getProperty(ModelProperty, Object)} and returns its value.
         * If no such model is present, the result is {@link Optional#empty()}.
         */
        <X, C> Optional<X> findProperty(ModelProperty<X,C> property, C context);

        /**
         * Finds the first model in the stack that returns a value for {@link ModelInstance#getProperty(ModelProperty)} and returns its value.
         * If no such model is present, the result is {@link Optional#empty()}.
         */
        default <X> Optional<X> findProperty(ModelProperty<X,Void> property){
            return this.findProperty(property, null);
        }
    }
}
