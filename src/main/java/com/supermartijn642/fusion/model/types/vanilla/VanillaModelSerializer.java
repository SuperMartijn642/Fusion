package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class VanillaModelSerializer implements JsonSerializer<CuboidModel> {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(CuboidModel.class, new VanillaModelSerializer()).disableHtmlEscaping().setPrettyPrinting().create();

    private VanillaModelSerializer(){
    }

    @Override
    public JsonElement serialize(CuboidModel src, Type typeOfSrc, JsonSerializationContext context){
        JsonObject json = new JsonObject();
        Identifier parent = src.parent();
        if(parent != null)
            json.addProperty("parent", parent.toString());
        if(!src.textureSlots().values().isEmpty()){
            JsonObject textures = new JsonObject();
            src.textureSlots().values().forEach((key, texture) -> textures.add(key, switch (texture){
                case TextureSlots.Reference(String target) -> new JsonPrimitive("#" + target);
                case TextureSlots.Value(Material material) -> Material.CODEC.encodeStart(JsonOps.INSTANCE, material).getOrThrow();
            }));
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
