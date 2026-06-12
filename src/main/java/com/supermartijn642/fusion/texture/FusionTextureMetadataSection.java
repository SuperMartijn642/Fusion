package com.supermartijn642.fusion.texture;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionTextureMetadataSection implements MetadataSectionSerializer<RawTextureInstance<?,?>> {

    public static final FusionTextureMetadataSection INSTANCE = new FusionTextureMetadataSection();

    @Override
    public String getMetadataSectionName(){
        return "fusion";
    }

    @Override
    public RawTextureInstance<?,?> fromJson(JsonObject json){
        // Get the texture type
        return TextureTypeRegistryImpl.deserializeTextureData(json);
    }
}
