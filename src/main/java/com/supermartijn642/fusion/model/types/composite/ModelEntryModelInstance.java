package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
public class ModelEntryModelInstance implements UntypedModelInstance {

    private final Either<ResourceLocation,ModelInstance<?>> model;
    private final ModelTransform transform;
    private final ModelPredicate condition;

    public ModelEntryModelInstance(Either<ResourceLocation,ModelInstance<?>> model, ModelTransform transform, ModelPredicate condition){
        this.model = model;
        this.transform = transform;
        this.condition = condition;
    }

    @Override
    public Collection<ResourceLocation> getDependencies(){
        return this.model.isLeft() ? List.of(this.model.left()) : List.of();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(){
        return List.of(this.model.map(l -> l, m -> m));
    }

    @Override
    public ModelTransform getTransform(){
        return this.transform;
    }

    @Override
    public @Nullable ModelPredicate getCondition(){
        return this.condition;
    }

    @Override
    public @Nullable BakedModel bakeModel(ModelBakingContext context, ModelStack modelStack){
        UntypedModelInstance model = this.model.flatMap(
            context::getModelOrMissing,
            m -> m
        );
        ResourceLocation identifier = this.model.leftOrNull();
        return model.bakeModel(context, modelStack.push(model, identifier));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return null;
    }

    @Override
    public BlockModel.@Nullable GuiLight getGuiLight(){
        return null;
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type){
        return null;
    }

    @Override
    public List<ItemOverride> getItemOverrides(){
        return List.of();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(){
        return Map.of();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(){
        return null;
    }

    @Override
    public @Nullable Boolean getShade(){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        return Optional.empty();
    }
}
