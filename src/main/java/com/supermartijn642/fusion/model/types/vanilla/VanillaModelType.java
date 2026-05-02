package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.client.resources.model.cuboid.CuboidModel;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType extends UnknownModelType<CuboidModel> {

    @Override
    public CuboidModel deserialize(JsonObject json) throws JsonParseException{
        return CuboidModel.GSON.fromJson(json, CuboidModel.class);
    }

    @Override
    public JsonObject serialize(CuboidModel value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }
}
