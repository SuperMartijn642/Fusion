package com.supermartijn642.fusion.entity.model.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * Created 17/09/2024 by SuperMartijn642
 */
public class OptifineEntityModelLoader implements EntityModelLoader{
    @Override
    public LayerDefinition loadModel(JsonObject json){
        throw new JsonParseException("OptiFine entity models are currently not supported.");
    }
}
