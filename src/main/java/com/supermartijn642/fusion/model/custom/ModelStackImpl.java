package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public class ModelStackImpl implements ModelStack {

    private static final ModelStack EMPTY = new ModelStackImpl();

    public static ModelStack empty(){
        return EMPTY;
    }

    private final ModelStackImpl[] stack;
    private final UntypedModelInstance model;
    private final ResourceLocation identifier;

    private ModelStackImpl(){
        this.stack = new ModelStackImpl[0];
        this.model = null;
        this.identifier = null;
    }

    private ModelStackImpl(ModelStackImpl[] stack, UntypedModelInstance model, ResourceLocation identifier){
        this.stack = new ModelStackImpl[stack.length + 1];
        System.arraycopy(stack, 0, this.stack, 0, stack.length);
        this.stack[stack.length] = this;
        this.model = model;
        this.identifier = identifier;
    }

    @Override
    public ModelStack push(UntypedModelInstance instance, @Nullable ResourceLocation identifier){
        return new ModelStackImpl(this.stack, instance, identifier);
    }

    @Override
    public int size(){
        return this.stack.length;
    }

    @Override
    public UntypedModelInstance get(int index){
        return this.stack[index].model;
    }

    @Nullable
    public ResourceLocation getIdentifier(int index){
        return this.stack[index].identifier;
    }

    @Override
    public @NotNull Iterator<UntypedModelInstance> iterator(){
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext(){
                return this.index < ModelStackImpl.this.stack.length;
            }

            @Override
            public UntypedModelInstance next(){
                if(this.index >= ModelStackImpl.this.stack.length)
                    throw new NoSuchElementException();
                return ModelStackImpl.this.stack[this.index++].model;
            }
        };
    }

    @Override
    public @Nullable Boolean findAmbientOcclusion(){
        for(ModelStackImpl entry : this.stack){
            Boolean value = entry.model.getAmbientOcclusion();
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable Boolean findAmbientOcclusionIncludingParents(ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getAmbientOcclusion();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public @Nullable UnbakedModel.GuiLight findGuiLight(){
        for(ModelStackImpl entry : this.stack){
            UnbakedModel.GuiLight value = entry.model.getGuiLight();
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable UnbakedModel.GuiLight findGuiLightIncludingParents(ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getGuiLight();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public @Nullable ItemTransform findItemTransform(ItemDisplayContext type){
        for(ModelStackImpl entry : this.stack){
            ItemTransform value = entry.model.getItemTransform(type);
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable ItemTransform findItemTransformIncludingParents(ItemDisplayContext type, ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getItemTransform(type);
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public @Nullable Either<String,ModelMaterial> findMaterial(String key){
        for(ModelStackImpl entry : this.stack){
            Either<String,ModelMaterial> value = entry.model.getMaterial(key);
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable Either<String,ModelMaterial> findMaterialIncludingParents(String key, ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getMaterial(key);
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> findMaterials(){
        Map<String,Either<String,ModelMaterial>> materials = new HashMap<>();
        for(int i = this.stack.length - 1; i >= 0; i--)
            materials.putAll(this.get(i).getMaterials());
        return materials;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> findMaterialsIncludingParents(ModelResolver modelResolver){
        Map<String,Either<String,ModelMaterial>> materials = new HashMap<>();
        this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                modelInstance.getMaterials().forEach(materials::putIfAbsent);
                return ModelWalker.Result.proceed();
            }
        );
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
    public @Nullable ModelMaterial findMaterialRecursiveIncludingParents(String key, Consumer<List<String>> reportCircular, ModelResolver modelResolver){
        List<String> encounteredKeys = new ArrayList<>();
        while(true){
            encounteredKeys.add(key);
            Either<String,ModelMaterial> next = this.findMaterialIncludingParents(key, modelResolver);
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
        for(ModelStackImpl entry : this.stack){
            ModelGeometry value = entry.model.getGeometry();
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable ModelGeometry findGeometryIncludingParents(ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getGeometry();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public @Nullable Boolean findShade(){
        for(ModelStackImpl entry : this.stack){
            Boolean value = entry.model.getShade();
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable Boolean findShadeIncludingParents(ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getShade();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public @Nullable Boolean findEmissive(){
        for(ModelStackImpl entry : this.stack){
            Boolean value = entry.model.getEmissive();
            if(value != null)
                return value;
        }
        return null;
    }

    @Override
    public @Nullable Boolean findEmissiveIncludingParents(ModelResolver modelResolver){
        return this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getEmissive();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(null);
    }

    @Override
    public ModelTransform composeTransforms(){
        ModelTransform transform = ModelTransform.identity();
        for(ModelStackImpl entry : this.stack)
            transform = ModelTransform.compose(entry.model.getTransform(), transform);
        return transform;
    }

    @Override
    public @Nullable ModelPredicate combineConditions(){
        List<ModelPredicate> predicates = new ArrayList<>(this.size());
        for(ModelStackImpl entry : this.stack){
            ModelPredicate condition = entry.model.getCondition();
            if(condition != null)
                predicates.add(condition);
        }
        return predicates.isEmpty() ? null : DefaultModelPredicates.and(predicates.toArray(new ModelPredicate[0]));
    }

    @Override
    public <X, C> Optional<X> findProperty(Property<X,C> property, C context){
        for(ModelStackImpl entry : this.stack){
            Optional<X> value = entry.model.getProperty(property, context);
            if(value.isPresent())
                return value;
        }
        if(property == DefaultModelProperties.MATERIAL)
            return DefaultModelProperties.MATERIAL.cast(this.findMaterial((String)context));
        return Optional.empty();
    }

    @Override
    public <X, C> Optional<X> findPropertyIncludingParents(Property<X,C> property, C context, ModelResolver modelResolver){
        Optional<X> value = this.walkStackAndParents(
            modelResolver,
            (modelInstance, stack) -> {
                var v = modelInstance.getProperty(property, context);
                return ModelWalker.Result.stopIfPresent(v);
            }
        );
        if(value.isEmpty() && property == DefaultModelProperties.MATERIAL)
            return DefaultModelProperties.MATERIAL.cast(this.findMaterialIncludingParents((String)context, modelResolver));
        return value;
    }

    @Override
    public <T> Optional<T> walkStack(ModelWalker<T> walker){
        ModelWalker.Result<T> result = this.walkStackInternal(walker);
        return result instanceof ModelWalkerImpl.StopResult(T value) ? Optional.ofNullable(value) : Optional.empty();
    }

    @Override
    public <T> Optional<T> walkStackAndParents(ModelResolver modelResolver, ModelWalker<T> walker){
        ModelWalker.Result<T> result = this.walkStackInternal(walker);
        if(result instanceof ModelWalkerImpl.StopResult(T value))
            return Optional.ofNullable(value);
        if(result == ModelWalker.Result.endBranch())
            return Optional.empty();
        return ModelWalkerImpl.walkParents(this, modelResolver, walker);
    }

    @Override
    public <T> Optional<T> walkParents(ModelResolver modelResolver, ModelWalker<T> walker){
        return ModelWalkerImpl.walkParents(this, modelResolver, walker);
    }

    private <T> ModelWalker.Result<T> walkStackInternal(ModelWalker<T> walker){
        ModelWalker.Result<T> result;
        for(ModelStackImpl entry : this.stack){
            result = walker.consume(entry.model, entry);
            if(result instanceof ModelWalkerImpl.StopResult)
                return result;
            if(result == ModelWalker.Result.endBranch())
                return result;
            if(result != ModelWalker.Result.proceed())
                throw new AssertionError("Unexpected result: " + result.getClass());
        }
        return null;
    }

    @Override
    public String toString(){
        return Arrays.stream(this.stack).map(entry -> {
            String identifier = entry.identifier == null ? "'unnamed'" : "'" + entry.identifier + "'";
            if(entry.model instanceof ModelInstance<?>){
                String type = "'" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)entry.model).getModelType()) + "'";
                return identifier + "@" + type;
            }
            return identifier;
        }).collect(Collectors.joining(" -> "));
    }
}
