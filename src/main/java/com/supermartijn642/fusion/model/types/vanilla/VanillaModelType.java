package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.client.renderer.block.model.BlockModel;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType extends UnknownModelType<BlockModel> {

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }
}
