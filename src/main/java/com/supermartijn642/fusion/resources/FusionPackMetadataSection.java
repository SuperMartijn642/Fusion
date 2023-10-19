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
        String overridesFolder = null;
        if(json.has("overrides_folder")){
            JsonElement element = json.get("overrides_folder");
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new RuntimeException("'overrides_folder' must be a string!");

            overridesFolder = element.getAsString().trim();
            if(!overridesFolder.matches("[a-z0-9/._-]+"))
                throw new RuntimeException("'overrides_folder' must be a valid path!");

            if(!overridesFolder.endsWith("/"))
                overridesFolder += "/";

            if(overridesFolder.startsWith("assets/"))
                throw new RuntimeException("'overrides_folder' cannot be inside 'assets'!");
            if(overridesFolder.startsWith("data/"))
                throw new RuntimeException("'overrides_folder' cannot be inside 'data'!");
        }
        return new Data(overridesFolder);
    }

    public static class Data implements IMetadataSection {

        public final String overridesFolder;

        public Data(String overridesFolder){
            this.overridesFolder = overridesFolder;
        }
    }
}
