package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelResolver;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.ModelWalker;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created 04/05/2026 by SuperMartijn642
 */
public class ModelWalkerImpl {

    private static final ModelWalker.Result<?> PROCEED = new ModelWalker.Result<Object>() {};
    private static final ModelWalker.Result<?> END_BRANCH = new ModelWalker.Result<Object>() {};
    private static final ModelWalker.Result<Void> EMPTY_STOP = new StopResult<>(null);

    public static <T> Optional<T> walkModelTree(ModelResolver modelResolver, UntypedModelInstance modelInstance, ModelWalker<T> walker){
        ModelStack stack = ModelStack.empty();
        ModelWalker.Result<T> result = walk(stack, modelResolver, Either.right(modelInstance), walker);
        return result instanceof StopResult<?> ? Optional.ofNullable(((StopResult<T>)result).value) : Optional.empty();
    }

    public static <T> Optional<T> walkModelTree(ModelResolver modelResolver, ResourceLocation model, ModelWalker<T> walker){
        ModelStack stack = ModelStack.empty();
        ModelWalker.Result<T> result = walk(stack, modelResolver, Either.left(model), walker);
        return result instanceof StopResult<?> ? Optional.ofNullable(((StopResult<T>)result).value) : Optional.empty();
    }

    public static <T> Optional<T> walkParents(ModelStack stack, ModelResolver modelResolver, ModelWalker<T> walker){
        UntypedModelInstance last = stack.get(stack.size() - 1);
        ModelWalker.Result<T> result;
        for(Either<ResourceLocation,UntypedModelInstance> parent : last.getParents()){
            result = walk(stack, modelResolver, parent, walker);
            if(result instanceof StopResult)
                return Optional.ofNullable(((StopResult<T>)result).value);
        }
        return Optional.empty();
    }

    private static <T> ModelWalker.Result<T> walk(ModelStack stack, ModelResolver modelResolver, Either<ResourceLocation,UntypedModelInstance> entry, ModelWalker<T> walker){
        UntypedModelInstance modelInstance = entry.flatMap(modelResolver::getModelOrMissing, Function.identity());
        stack = stack.push(modelInstance, entry.leftOrNull());
        ModelWalker.Result<T> result = walker.consume(modelInstance, stack);
        if(result instanceof StopResult)
            return result;
        if(result == END_BRANCH)
            return null;
        if(result != PROCEED)
            throw new AssertionError("Unexpected result: " + result.getClass());
        for(Either<ResourceLocation,UntypedModelInstance> parent : modelInstance.getParents()){
            result = walk(stack, modelResolver, parent, walker);
            if(result instanceof StopResult)
                return result;
        }
        return null;
    }

    public static <T> ModelWalker.Result<T> proceed(){
        //noinspection unchecked
        return (ModelWalker.Result<T>)PROCEED;
    }

    public static <T> ModelWalker.Result<T> endBranch(){
        //noinspection unchecked
        return (ModelWalker.Result<T>)END_BRANCH;
    }

    public static <T> ModelWalker.Result<T> stop(T value){
        return new StopResult<>(value);
    }

    public static ModelWalker.Result<Void> stop(){
        return EMPTY_STOP;
    }

    static class StopResult<T> implements ModelWalker.Result<T> {
        private final T value;

        private StopResult(T value){
            this.value = value;
        }

        public T value(){
            return this.value;
        }
    }
}
