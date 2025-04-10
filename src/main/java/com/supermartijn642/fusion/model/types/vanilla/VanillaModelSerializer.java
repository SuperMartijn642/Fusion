package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.*;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.Locale;

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
        ResourceLocation parent = src.parent();
        if(parent != null)
            json.addProperty("parent", parent.toString());
        if(!src.textureSlots().values().isEmpty()){
            JsonObject textures = new JsonObject();
            src.textureSlots().values().forEach((key, texture) -> textures.addProperty(key, texture instanceof TextureSlots.Value ? ((TextureSlots.Value)texture).material().texture().toString() : '#' + ((TextureSlots.Reference)texture).target()));
            json.add("textures", textures);
        }
        if(src.ambientOcclusion() != null)
            json.addProperty("ambientocclusion", src.ambientOcclusion());
        UnbakedModel.GuiLight guiLight = src.guiLight();
        if(guiLight != null)
            json.addProperty("gui_light", guiLight.name().toLowerCase(Locale.ROOT));
        return json;
    }
}
