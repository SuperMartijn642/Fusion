package com.supermartijn642.fusion.model.types.cuboid;

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
public class CuboidModelSerializer implements JsonSerializer<BlockModel> {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(BlockModel.class, new CuboidModelSerializer()).disableHtmlEscaping().setPrettyPrinting().create();

    private CuboidModelSerializer(){
    }

    @Override
    public JsonElement serialize(BlockModel src, Type typeOfSrc, JsonSerializationContext context){
        JsonObject json = new JsonObject();
        ResourceLocation parent = src.getParentLocation();
        if(parent != null)
            json.addProperty("parent", parent.toString());
        if(!src.getTextureSlots().values().isEmpty()){
            JsonObject textures = new JsonObject();
            src.getTextureSlots().values().forEach((key, texture) -> {
                String value = texture instanceof TextureSlots.Value ?
                    ((TextureSlots.Value)texture).material().texture().toString() :
                    '#' + ((TextureSlots.Reference)texture).target();
                textures.addProperty(key, value);
            });
            json.add("textures", textures);
        }
        if(src.getAmbientOcclusion() != null)
            json.addProperty("ambientocclusion", src.getAmbientOcclusion());
        UnbakedModel.GuiLight guiLight = src.getGuiLight();
        if(guiLight != null)
            json.addProperty("gui_light", guiLight.name().toLowerCase(Locale.ROOT));
        return json;
    }
}
