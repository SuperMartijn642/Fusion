package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelResolver;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.ModelWalker;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created 04/05/2026 by SuperMartijn642
 */
public class ModelWalkerImpl {

    private static final ModelWalker.Result<?> PROCEED = new ModelWalker.Result<>() {};
    private static final ModelWalker.Result<?> END_BRANCH = new ModelWalker.Result<>() {};
    private static final ModelWalker.Result<Void> EMPTY_STOP = new StopResult<>(null);

    public static <T> Optional<T> walkModelTree(ModelResolver modelResolver, UntypedModelInstance modelInstance, ModelWalker<T> walker){
        ModelStack stack = ModelStack.empty();
        return walk(stack, modelResolver, Either.right(modelInstance), walker) instanceof StopResult(T value) ? Optional.ofNullable(value) : Optional.empty();
    }

    public static <T> Optional<T> walkModelTree(ModelResolver modelResolver, Identifier model, ModelWalker<T> walker){
        ModelStack stack = ModelStack.empty();
        return walk(stack, modelResolver, Either.left(model), walker) instanceof StopResult(T value) ? Optional.ofNullable(value) : Optional.empty();
    }

    public static <T> Optional<T> walkParents(ModelStack stack, ModelResolver modelResolver, ModelWalker<T> walker){
        UntypedModelInstance last = stack.get(stack.size() - 1);
        ModelWalker.Result<T> result;
        for(Either<Identifier,UntypedModelInstance> parent : last.getParents()){
            result = walk(stack, modelResolver, parent, walker);
            if(result instanceof StopResult(T value))
                return Optional.ofNullable(value);
        }
        return Optional.empty();
    }

    private static <T> ModelWalker.Result<T> walk(ModelStack stack, ModelResolver modelResolver, Either<Identifier,UntypedModelInstance> entry, ModelWalker<T> walker){
        UntypedModelInstance modelInstance = entry.flatMap(modelResolver::getModelOrMissing, Function.identity());
        stack = stack.push(modelInstance, entry.leftOrNull());
        ModelWalker.Result<T> result = walker.consume(modelInstance, stack);
        if(result instanceof StopResult)
            return result;
        if(result == END_BRANCH)
            return null;
        if(result != PROCEED)
            throw new AssertionError("Unexpected result: " + result.getClass());
        for(Either<Identifier,UntypedModelInstance> parent : modelInstance.getParents()){
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

    record StopResult<T>(T value) implements ModelWalker.Result<T> {
    }
}
