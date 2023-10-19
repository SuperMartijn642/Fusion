package com.supermartijn642.fusion.texture;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSectionSerializer;

import java.lang.reflect.Type;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionTextureMetadataSection implements IMetadataSectionSerializer<FusionTextureMetadataSection.Data> {

    public static final FusionTextureMetadataSection INSTANCE = new FusionTextureMetadataSection();
    private static boolean registered = false;

    public static synchronized void registerMetadata(){
        if(!registered){
            ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).rmMetadataSerializer.registerMetadataSectionType(FusionTextureMetadataSection.INSTANCE, FusionTextureMetadataSection.Data.class);
            registered = true;
        }
    }

    @Override
    public String getSectionName(){
        return "fusion";
    }

    @Override
    public Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException{
        // Finalize the registry
        TextureTypeRegistryImpl.finalizeRegistration();
        // Get the texture type
        return new Data(TextureTypeRegistryImpl.deserializeTextureData(json.getAsJsonObject()));
    }

    public static class Data implements IMetadataSection {

        public final Pair<TextureType<Object>,Object> pair;

        public Data(Pair<TextureType<Object>,Object> pair){
            this.pair = pair;
        }
    }
}
