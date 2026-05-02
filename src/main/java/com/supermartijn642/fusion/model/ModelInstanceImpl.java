package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.Collection;
import java.util.List;

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
    public Collection<Identifier> getModelDependencies(){
        return this.modelType.getModelDependencies(this.modelData);
    }

    @Override
    public @Nullable UnbakedModel getAsVanillaModel(){
        return this.modelType.getAsVanillaModel(this.modelData);
    }

    @Override
    public BlockStateModel bakeBlockModel(BlockModelBakingContext context){
        return this.modelType.bakeBlockModel(context, this.modelData);
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, Matrix4fc transformation){
        return this.modelType.bakeItemModel(context, this.modelData, transformation);
    }

    @Override
    public List<Identifier> getParentModels(){
        return this.modelType.getParentModels(this.modelData);
    }
}
