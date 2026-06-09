package com.supermartijn642.fusion.model.types;

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
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends IModel> implements ModelType<T> {

    @Override
    public Collection<ResourceLocation> getDependencies(T data){
        return data.getDependencies();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(T data){
        return Collections.emptyList();
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.asVanillaModel().map(DefaultModelTypes.CUBOID::getAmbientOcclusion).orElse(null);
    }

    @Override
    public Boolean getIsGui3d(T data){
        return data.asVanillaModel().map(DefaultModelTypes.CUBOID::getIsGui3d).orElse(null);
    }

    @Override
    public ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, T data){
        return data.asVanillaModel().map(m -> DefaultModelTypes.CUBOID.getItemTransform(type, m)).orElse(null);
    }

    @Override
    public List<ItemOverride> getItemOverrides(T data){
        return Collections.emptyList();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return data.asVanillaModel().map(DefaultModelTypes.CUBOID::getMaterials).orElse(Collections.emptyMap());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        return ModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, T data){
        // Bake the model
        Function<ResourceLocation,TextureAtlasSprite> spriteGetter = material -> context.getMaterial(ModelMaterial.of(material));
        return data.bake(context.getTransformation().toModelState(), DefaultVertexFormats.ITEM, spriteGetter);
    }

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }
}
