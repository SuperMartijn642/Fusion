package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.ExtendedBlockModelDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType implements ModelType<BlockModel> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(BlockModel data){
        return data.getDependencies();
    }

    @Override
    public BakedModel bake(ModelBakingContext context, BlockModel data){
        if(data.parentLocation != null && data.parent == null){
            ModelInstance<?> model = context.getModel(data.parentLocation);
            if(model != null)
                data.parent = model.getAsVanillaModel();
        }
        return data.bake(context.getModelBaker(), material -> context.getTexture(SpriteIdentifier.of(material)), context.getTransformation(), context.getModelIdentifier());
    }

    @Nullable
    @Override
    public BlockModel getAsVanillaModel(BlockModel data){
        return data;
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return ExtendedBlockModelDeserializer.INSTANCE.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }
}
