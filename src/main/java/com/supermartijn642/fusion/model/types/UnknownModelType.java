package com.supermartijn642.fusion.model.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.GatherTexturesContext;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType implements ModelType<IModel> {

    @Override
    public IModel deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(IModel value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }

    @Override
    public Collection<ResourceLocation> getModelDependencies(IModel data){
        return data.getDependencies();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, IModel data){
        // Get the textures
        Collection<ResourceLocation> materials = data.getTextures();
        return materials.stream().map(i -> SpriteIdentifier.of(TextureAtlases.getBlocks(), i)).collect(Collectors.toList());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, IModel data){
        return data.bake(context.getTransformation(), DefaultVertexFormats.BLOCK, material -> context.getTexture(SpriteIdentifier.of(TextureAtlases.getBlocks(), material)));
    }
}
