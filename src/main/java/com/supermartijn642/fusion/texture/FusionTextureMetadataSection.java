package com.supermartijn642.fusion.texture;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.resources.data.IMetadataSectionSerializer;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionTextureMetadataSection implements IMetadataSectionSerializer<Pair<TextureType<Object>,Object>> {

    public static final FusionTextureMetadataSection INSTANCE = new FusionTextureMetadataSection();

    @Override
    public String getMetadataSectionName(){
        return "fusion";
    }

    @Override
    public Pair<TextureType<Object>,Object> fromJson(JsonObject json){
        // Finalize the registry
        TextureTypeRegistryImpl.finalizeRegistration();
        // Get the texture type
        return TextureTypeRegistryImpl.deserializeTextureData(json);
    }
}
