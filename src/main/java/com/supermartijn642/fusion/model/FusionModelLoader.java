package com.supermartijn642.fusion.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
public class FusionModelLoader implements UnbakedModelLoader<BlockModel> {
    @Override
    public BlockModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException{
        // Load the model data
        ModelInstance<?> model = ModelTypeRegistryImpl.deserializeModelData(json.getAsJsonObject());

        // Create a dummy block model
        return new FusionBlockModelData(model);
    }
}
