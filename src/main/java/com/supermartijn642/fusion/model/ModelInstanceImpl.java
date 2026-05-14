package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
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
    public Collection<ResourceLocation> getDependencies(){
        return this.modelType.getDependencies(this.modelData);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(){
        return this.modelType.getParents(this.modelData);
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return this.modelType.getAmbientOcclusion(this.modelData);
    }

    @Override
    public @Nullable Boolean getIsGui3d(){
        return this.modelType.getIsGui3d(this.modelData);
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type){
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
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        return this.modelType.getProperty(property, context, this.modelData);
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context){
        return this.modelType.bakeModel(context, this.modelData);
    }
}
