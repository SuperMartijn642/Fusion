package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.ModelWalker;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 04/05/2026 by SuperMartijn642
 */
public class ModelWalkerImpl {

    private static final ModelWalker.Result<?> PROCEED = new ModelWalker.Result<>() {};
    private static final ModelWalker.Result<?> END_BRANCH = new ModelWalker.Result<>() {};
    private static final ModelWalker.Result<Void> EMPTY_STOP = new StopResult<>(null);

    public static <T> Optional<T> walkModelTree(Function<ResourceLocation,ModelInstance<?>> modelResolver, UntypedModelInstance modelInstance, ModelWalker<T> walker){
        ModelStackImpl stack = new ModelStackImpl();
        return walk(stack, modelResolver::apply, Either.right(modelInstance), walker) instanceof StopResult(T value) ? Optional.ofNullable(value) : Optional.empty();
    }

    public static <T> Optional<T> walkModelTree(Function<ResourceLocation,ModelInstance<?>> modelResolver, ResourceLocation model, ModelWalker<T> walker){
        ModelStackImpl stack = new ModelStackImpl();
        return walk(stack, modelResolver::apply, Either.left(model), walker) instanceof StopResult(T value) ? Optional.ofNullable(value) : Optional.empty();
    }

    private static <T> ModelWalker.Result<T> walk(ModelStackImpl stack, Function<ResourceLocation,UntypedModelInstance> modelResolver, Either<ResourceLocation,UntypedModelInstance> entry, ModelWalker<T> walker){
        UntypedModelInstance modelInstance = entry.flatMap(modelResolver, Function.identity());
        stack.push(entry.leftOrNull(), modelInstance);
        ModelWalker.Result<T> result = walker.consume(modelInstance, stack);
        if(result instanceof StopResult)
            return result;
        if(result == END_BRANCH){
            stack.pop();
            return null;
        }
        if(result != PROCEED)
            throw new AssertionError("Unexpected result: " + result.getClass());
        for(Either<ResourceLocation,UntypedModelInstance> parent : modelInstance.getParents()){
            result = walk(stack, modelResolver, parent, walker);
            if(result instanceof StopResult)
                return result;
        }
        stack.pop();
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

    private record StopResult<T>(T value) implements ModelWalker.Result<T> {
    }

    private static class ModelStackImpl implements ModelWalker.ModelStack {

        private final List<Pair<ResourceLocation,UntypedModelInstance>> models = new ArrayList<>(); // Model trees aren't expected to get very tall, hence an array list should be fine

        void push(ResourceLocation identifier, UntypedModelInstance modelInstance){
            this.models.add(Pair.of(identifier, modelInstance));
        }

        void pop(){
            this.models.removeLast();
        }

        @Override
        public int size(){
            return this.models.size();
        }

        @Override
        public UntypedModelInstance get(int index){
            return this.models.get(index).right();
        }

        @Nullable
        public ResourceLocation getIdentifier(int index){
            return this.models.get(index).left();
        }

        @Override
        public @NotNull Iterator<UntypedModelInstance> iterator(){
            Iterator<Pair<ResourceLocation,UntypedModelInstance>> iterator = this.models.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext(){
                    return iterator.hasNext();
                }

                @Override
                public UntypedModelInstance next(){
                    return iterator.next().right();
                }
            };
        }

        @Override
        public @Nullable Boolean findAmbientOcclusion(){
            for(UntypedModelInstance model : this){
                Boolean value = model.getAmbientOcclusion();
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public @Nullable BlockModel.GuiLight findGuiLight(){
            for(UntypedModelInstance model : this){
                BlockModel.GuiLight value = model.getGuiLight();
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public @Nullable ItemTransform findItemTransform(ItemDisplayContext type){
            for(UntypedModelInstance model : this){
                ItemTransform value = model.getItemTransform(type);
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public @Nullable Either<String,ModelMaterial> findMaterial(String key){
            for(UntypedModelInstance model : this){
                Either<String,ModelMaterial> value = model.getMaterial(key);
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public Map<String,Either<String,ModelMaterial>> findMaterials(){
            Map<String,Either<String,ModelMaterial>> materials = new HashMap<>();
            for(int i = this.models.size() - 1; i >= 0; i--)
                materials.putAll(this.get(i).getMaterials());
            return materials;
        }

        @Override
        public @Nullable ModelMaterial findMaterialRecursive(String key, Consumer<List<String>> reportCircular){
            List<String> encounteredKeys = new ArrayList<>();
            while(true){
                encounteredKeys.add(key);
                Either<String,ModelMaterial> next = this.findMaterial(key);
                if(next == null)
                    break;
                if(next.isRight())
                    return next.right();
                key = next.left();
                if(encounteredKeys.contains(key)){
                    encounteredKeys.add(key);
                    reportCircular.accept(Collections.unmodifiableList(encounteredKeys));
                    break;
                }
            }
            return null;
        }

        @Override
        public @Nullable ModelGeometry findGeometry(){
            for(UntypedModelInstance model : this){
                ModelGeometry value = model.getGeometry();
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public @Nullable Boolean findShade(){
            for(UntypedModelInstance model : this){
                Boolean value = model.getShade();
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public @Nullable Boolean findEmissive(){
            for(UntypedModelInstance model : this){
                Boolean value = model.getEmissive();
                if(value != null)
                    return value;
            }
            return null;
        }

        @Override
        public ModelTransform composeTransforms(){
            ModelTransform transform = ModelTransform.identity();
            for(UntypedModelInstance model : this)
                transform = ModelTransform.compose(model.getTransform(), transform);
            return transform;
        }

        @Override
        public @Nullable ModelPredicate combineConditions(){
            List<ModelPredicate> predicates = new ArrayList<>(this.size());
            for(UntypedModelInstance model : this){
                ModelPredicate condition = model.getCondition();
                if(condition != null)
                    predicates.add(condition);
            }
            return predicates.isEmpty() ? null : DefaultModelPredicates.and(predicates.toArray(new ModelPredicate[0]));
        }

        @Override
        public <X, C> Optional<X> findProperty(Property<X,C> property, C context){
            for(UntypedModelInstance model : this){
                Optional<X> value = model.getProperty(property, context);
                if(value.isPresent())
                    return value;
            }
            return Optional.empty();
        }

        @Override
        public String toString(){
            return this.models.stream().map(pair -> {
                String identifier = pair.left() == null ? "'unnamed'" : "'" + pair.left() + "'";
                if(pair.right() instanceof ModelInstance<?>){
                    String type = "'" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)pair.right()).getModelType()) + "'";
                    return identifier + "@" + type;
                }
                return identifier;
            }).collect(Collectors.joining(" -> "));
        }
    }
}
