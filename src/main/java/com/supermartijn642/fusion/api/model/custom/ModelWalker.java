package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.model.custom.ModelWalkerImpl;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

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
    static <T> Optional<T> walkModelTree(ModelResolver modelResolver, UntypedModelInstance modelInstance, ModelWalker<T> walker){
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
    static <T> Optional<T> walkModelTree(ModelResolver modelResolver, ResourceLocation model, ModelWalker<T> walker){
        return ModelWalkerImpl.walkModelTree(modelResolver, model, walker);
    }

    /**
     * Consumes one model in the model tree.
     * @param modelInstance the model instance
     * @param stack         the current stack of models
     * @return {@link Result#proceed()} when the walker should continue, {@link Result#endBranch()} when the walker should end exploring the current branch and continue with the next branch, and {@link Result#stop(Object)} when the walker should stop
     * @see Result
     */
    Result<T> consume(UntypedModelInstance modelInstance, ModelStack stack);

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
         * The walker should stop and return the given value is present, and proceed otherwise.
         */
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        static <T> Result<T> stopIfPresent(Optional<T> value){
            return value.map(Result::stop).orElseGet(Result::proceed);
        }

        /**
         * The walker should stop.
         */
        static Result<Void> stop(){
            return ModelWalkerImpl.stop();
        }
    }
}
