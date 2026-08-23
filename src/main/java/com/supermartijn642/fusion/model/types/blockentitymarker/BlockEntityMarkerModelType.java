package com.supermartijn642.fusion.model.types.blockentitymarker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 23/08/2026 by SuperMartijn642
 */
public class BlockEntityMarkerModelType implements ModelType<Void> {

    @Override
    public Collection<ResourceLocation> getDependencies(Void data){
        return DefaultModelTypes.CUBOID.getDependencies(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(Void data){
        return DefaultModelTypes.CUBOID.getParents(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(Void data){
        return DefaultModelTypes.CUBOID.getAmbientOcclusion(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable Boolean getIsGui3d(Void data){
        return DefaultModelTypes.CUBOID.getIsGui3d(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, Void data){
        return DefaultModelTypes.CUBOID.getItemTransform(type, ModelBakery.MODEL_ENTITY);
    }

    @Override
    public List<ItemOverride> getItemOverrides(Void data){
        return DefaultModelTypes.CUBOID.getItemOverrides(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(Void data){
        return DefaultModelTypes.CUBOID.getMaterials(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable ModelGeometry getGeometry(Void data){
        return DefaultModelTypes.CUBOID.getGeometry(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable Boolean getShade(Void data){
        return DefaultModelTypes.CUBOID.getShade(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable Boolean getEmissive(Void data){
        return DefaultModelTypes.CUBOID.getEmissive(ModelBakery.MODEL_ENTITY);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, Void data){
        return DefaultModelTypes.CUBOID.getProperty(property, context, ModelBakery.MODEL_ENTITY);
    }

    @Override
    public @Nullable IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, Void data){
        return new BuiltInModel(
            SimpleModelType.resolveItemTransforms(context, modelStack),
            new ItemOverrideList(this.getItemOverrides(data))
        );
    }

    @Override
    public Void deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize block entity marker!");
    }

    @Override
    public JsonObject serialize(Void data){
        throw new UnsupportedOperationException("Cannot serialize block entity marker!");
    }
}
