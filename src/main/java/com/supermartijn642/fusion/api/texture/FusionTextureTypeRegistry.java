package com.supermartijn642.fusion.api.texture;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public final class FusionTextureTypeRegistry {

    /**
     * Registers the given texture type.
     * @param identifier  identifier for the texture type
     * @param textureType handler for custom texture data and creating the sprite
     */
    public static void registerTextureType(ResourceLocation identifier, TextureType<?> textureType){
        TextureTypeRegistryImpl.registerTextureType(identifier, textureType);
    }

    /**
     * Serializes the given texture data.
     * @param textureType type of the texture
     * @param textureData texture data to serialize
     */
    public static <T> JsonObject serializeTextureData(TextureType<T> textureType, T textureData){
        return TextureTypeRegistryImpl.serializeTextureData(textureType, textureData);
    }
}
