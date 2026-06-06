package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class ModelInstanceImpl<T> implements ModelInstance<T> {

    private final ModelType<T> modelType;
    private final T modelData;

    public ModelInstanceImpl(ModelType<T> modelType, T modelData){
        this.modelType = modelType;
        this.modelData = modelData;
    }

    @Override
    public ModelType<T> getModelType(){
        return this.modelType;
    }

    @Override
    public T getModelData(){
        return this.modelData;
    }

    @Override
    public Collection<Identifier> getDependencies(){
        return this.modelType.getDependencies(this.modelData);
    }

    @Override
    public List<Either<Identifier,UntypedModelInstance>> getParents(){
        return this.modelType.getParents(this.modelData);
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return this.modelType.getAmbientOcclusion(this.modelData);
    }

    @Override
    public @Nullable UnbakedModel.GuiLight getGuiLight(){
        return this.modelType.getGuiLight(this.modelData);
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type){
        return this.modelType.getItemTransform(type, this.modelData);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(){
        return this.modelType.getMaterials(this.modelData);
    }

    @Override
    public @Nullable ModelGeometry getGeometry(){
        return this.modelType.getGeometry(this.modelData);
    }

    @Override
    public @Nullable Boolean getShade(){
        return this.modelType.getShade(this.modelData);
    }

    @Override
    public @Nullable Boolean getEmissive(){
        return this.modelType.getEmissive(this.modelData);
    }

    @Override
    public ModelTransform getTransform(){
        return this.modelType.getTransform(this.modelData);
    }

    @Override
    public @Nullable ModelPredicate getCondition(){
        return this.modelType.getCondition(this.modelData);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        return this.modelType.getProperty(property, context, this.modelData);
    }

    @Override
    public @Nullable BlockStateModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack){
        return this.modelType.bakeBlockStateModel(context, modelStack, this.modelData);
    }

    @Override
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack){
        return this.modelType.bakeItemModel(context, modelStack, this.modelData);
    }
}
