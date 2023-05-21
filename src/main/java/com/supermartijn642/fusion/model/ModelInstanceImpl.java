package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

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
    public Collection<ResourceLocation> getModelDependencies(){
        return this.modelType.getModelDependencies(this.modelData);
    }

    @Override
    public @Nullable BlockModel getAsVanillaModel(){
        return this.modelType.getAsVanillaModel(this.modelData);
    }

    @Override
    public BakedModel bake(ModelBakingContext context){
        return this.modelType.bake(context, this.modelData);
    }
}
