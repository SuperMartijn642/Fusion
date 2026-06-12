package com.supermartijn642.fusion.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.util.CodecHelper;
import net.minecraft.server.packs.metadata.MetadataSectionType;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionTextureMetadataSection {

    private static final Codec<RawTextureInstance<?,?>> CODEC = CodecHelper.jsonSerializerToCodec(
        FusionTextureMetadataSection::toJson,
        FusionTextureMetadataSection::fromJson
    );
    public static final MetadataSectionType<RawTextureInstance<?,?>> TYPE = new MetadataSectionType<>("fusion", CODEC);

    private static JsonObject toJson(RawTextureInstance<?,?> data){
        return TextureTypeRegistryImpl.serializeTextureData(data);
    }

    private static RawTextureInstance<?,?> fromJson(JsonElement element){
        if(!element.isJsonObject())
            throw new JsonParseException("Fusion metadata section must be an object!");
        // Get the texture type
        return TextureTypeRegistryImpl.deserializeTextureData(element.getAsJsonObject());
    }
}
