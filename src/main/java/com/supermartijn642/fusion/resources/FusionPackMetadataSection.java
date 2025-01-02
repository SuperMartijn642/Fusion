package com.supermartijn642.fusion.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.supermartijn642.fusion.util.CodecHelper;
import net.minecraft.server.packs.metadata.MetadataSectionType;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
public class FusionPackMetadataSection {

    private static final Codec<FusionPackMetadata> CODEC = CodecHelper.jsonSerializerToCodec(
        FusionPackMetadataSection::toJson,
        FusionPackMetadataSection::fromJson
    );
    public static final MetadataSectionType<FusionPackMetadata> TYPE = new MetadataSectionType<>("fusion", CODEC);

    private static JsonObject toJson(FusionPackMetadata metadata){
        JsonObject json = new JsonObject();
        json.addProperty("minimum_version", metadata.getMinimumVersion());
        json.addProperty("overrides_folder", metadata.getOverridesFolder());
        return json;
    }

    private static FusionPackMetadata fromJson(JsonElement e){
        if(!e.isJsonObject())
            throw new JsonParseException("Fusion metadata section must be an object!");
        JsonObject json = e.getAsJsonObject();

        // Minimum version
        String minimumVersion = "1.0.0";
        if(json.has("minimum_version")){
            if(!json.get("minimum_version").isJsonPrimitive() || !json.getAsJsonPrimitive("minimum_version").isString())
                throw new JsonParseException("Property 'minimum_version' must be a string!");
            minimumVersion = json.get("minimum_version").getAsString();
            if(!minimumVersion.matches("\\d+\\.\\d+\\.\\d+([a-z].*|[+-].+)?"))
                throw new JsonParseException("Property 'minimum_version' must be a valid Fusion version, not '" + minimumVersion + "'!");
        }

        // Overrides folder
        String overridesFolder = null;
        if(json.has("overrides_folder")){
            JsonElement element = json.get("overrides_folder");
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new RuntimeException("Property 'overrides_folder' must be a string!");

            overridesFolder = element.getAsString().trim();
            if(!overridesFolder.matches("[a-z0-9/._-]+"))
                throw new RuntimeException("Property 'overrides_folder' must be a valid path!");

            if(!overridesFolder.endsWith("/"))
                overridesFolder += "/";

            if(overridesFolder.startsWith("assets/"))
                throw new RuntimeException("'overrides_folder' cannot be inside 'assets'!");
            if(overridesFolder.startsWith("data/"))
                throw new RuntimeException("'overrides_folder' cannot be inside 'data'!");
        }
        return new FusionPackMetadata(minimumVersion, overridesFolder);
    }
}
