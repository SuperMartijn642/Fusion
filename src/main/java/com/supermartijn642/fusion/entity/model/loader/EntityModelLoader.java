package com.supermartijn642.fusion.entity.model.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Created 17/09/2024 by SuperMartijn642
 */
public interface EntityModelLoader {

    ModelPart loadModel(JsonObject json) throws JsonParseException;
}
