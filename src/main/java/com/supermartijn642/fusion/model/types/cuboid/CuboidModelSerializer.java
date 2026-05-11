package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.*;
import net.minecraft.client.renderer.block.model.ModelBlock;

import java.lang.reflect.Type;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class CuboidModelSerializer implements JsonSerializer<ModelBlock> {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ModelBlock.class, new CuboidModelSerializer()).disableHtmlEscaping().setPrettyPrinting().create();

    private CuboidModelSerializer(){
    }

    @Override
    public JsonElement serialize(ModelBlock src, Type typeOfSrc, JsonSerializationContext context){
        JsonObject json = new JsonObject();
        if(src.parentLocation != null)
            json.addProperty("parent", src.parentLocation.toString());
        if(!src.textures.isEmpty()){
            JsonObject textures = new JsonObject();
            src.textures.forEach(textures::addProperty);
            json.add("textures", textures);
        }
        if(src.parentLocation == null && !src.ambientOcclusion)
            json.addProperty("ambientocclusion", false);
        return json;
    }
}
