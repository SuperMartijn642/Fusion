package com.supermartijn642.fusion.api.texture;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.resources.Identifier;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public final class FusionTextureTypeRegistry {

    /**
     * Registers the given texture type.
     * @param identifier  identifier for the texture type
     * @param textureType handler for custom texture data and creating the sprite
     */
    public static void registerTextureType(Identifier identifier, TextureType<?,?> textureType){
        TextureTypeRegistryImpl.registerTextureType(identifier, textureType);
    }

    /**
     * Serializes the given texture data.
     * @param texture texture to be serialized
     */
    public static JsonObject serializeTextureData(RawTextureInstance<?,?> texture){
        return TextureTypeRegistryImpl.serializeTextureData(texture);
    }
}
