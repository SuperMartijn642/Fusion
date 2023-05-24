package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.*;
import net.minecraft.client.renderer.model.BlockModel;

import java.lang.reflect.Type;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class VanillaModelSerializer implements JsonSerializer<BlockModel> {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(BlockModel.class, new VanillaModelSerializer()).disableHtmlEscaping().setPrettyPrinting().create();

    private VanillaModelSerializer(){
    }

    @Override
    public JsonElement serialize(BlockModel src, Type typeOfSrc, JsonSerializationContext context){
        JsonObject json = new JsonObject();
        if(src.parentLocation != null)
            json.addProperty("parent", src.parentLocation.toString());
        if(!src.textureMap.isEmpty()){
            JsonObject textures = new JsonObject();
            src.textureMap.forEach(textures::addProperty);
            json.add("textures", textures);
        }
        if(src.parentLocation == null && !src.hasAmbientOcclusion)
            json.addProperty("ambientocclusion", false);
        return json;
    }
}
