package com.supermartijn642.fusion.resources;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSectionSerializer;

import java.lang.reflect.Type;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
public class FusionPackMetadataSection implements IMetadataSectionSerializer<FusionPackMetadataSection.Data> {

    public static final FusionPackMetadataSection INSTANCE = new FusionPackMetadataSection();

    @Override
    public String getSectionName(){
        return "fusion";
    }

    @Override
    public Data deserialize(JsonElement source, Type type, JsonDeserializationContext context) throws JsonParseException{
        JsonObject json = source.getAsJsonObject();

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
        return new Data(new FusionPackMetadata(minimumVersion, overridesFolder));
    }

    public static class Data implements IMetadataSection {

        public final FusionPackMetadata metadata;

        public Data(FusionPackMetadata metadata){
            this.metadata = metadata;
        }
    }
}
