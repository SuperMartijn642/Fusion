package com.supermartijn642.fusion.model.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.GatherTexturesContext;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType implements ModelType<IUnbakedModel> {

    @Override
    public IUnbakedModel deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(IUnbakedModel value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }

    @Override
    public Collection<ResourceLocation> getModelDependencies(IUnbakedModel data){
        return data.getDependencies();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, IUnbakedModel data){
        // Get the textures
        Set<Pair<String,String>> errors = new HashSet<>();
        Collection<Material> materials = data.getMaterials(location -> context.getModel(location).getAsVanillaModel(), errors);
        return materials.stream().map(SpriteIdentifier::of).collect(Collectors.toList());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, IUnbakedModel data){
        return data.bake(context.getModelBakery(), material -> context.getTexture(SpriteIdentifier.of(material)), context.getTransformation(), context.getModelIdentifier());
    }
}
