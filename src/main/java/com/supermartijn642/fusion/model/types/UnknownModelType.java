package com.supermartijn642.fusion.model.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType implements ModelType<UnbakedModel> {

    @Override
    public UnbakedModel deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(UnbakedModel value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }

    @Override
    public Collection<ResourceLocation> getModelDependencies(UnbakedModel data){
        return data.getDependencies();
    }

    @Override
    public BakedModel bake(ModelBakingContext context, UnbakedModel data){
        return data.bake(context.getModelBaker(), material -> context.getTexture(SpriteIdentifier.of(material)), context.getTransformation());
    }
}
