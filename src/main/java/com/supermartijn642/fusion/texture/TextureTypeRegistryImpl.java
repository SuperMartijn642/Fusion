package com.supermartijn642.fusion.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class TextureTypeRegistryImpl {

    private static final Map<Identifier,TextureType<?,?>> IDENTIFIER_TO_TEXTURE_TYPE = new HashMap<>();
    private static final Map<TextureType<?,?>,Identifier> TEXTURE_TYPE_TO_IDENTIFIER = new HashMap<>();
    private static boolean finalized = false;

    public static synchronized void registerTextureType(Identifier identifier, TextureType<?,?> textureType){
        if(finalized)
            throw new RuntimeException("Texture types must be registered before textures get loaded!");
        if(IDENTIFIER_TO_TEXTURE_TYPE.containsKey(identifier))
            throw new RuntimeException("Duplicate texture type registration for identifier '" + identifier + "'!");
        if(TEXTURE_TYPE_TO_IDENTIFIER.containsKey(textureType))
            throw new RuntimeException("Texture type has already been registered!");

        IDENTIFIER_TO_TEXTURE_TYPE.put(identifier, textureType);
        TEXTURE_TYPE_TO_IDENTIFIER.put(textureType, identifier);
    }

    public static JsonObject serializeTextureData(RawTextureInstance<?,?> texture){
        if(!finalized)
            throw new RuntimeException("Can only serialize texture data after registration has completed!");
        Identifier identifier = TEXTURE_TYPE_TO_IDENTIFIER.get(texture.getTextureType());
        if(identifier == null)
            throw new RuntimeException("Cannot use unregistered texture type '" + texture.getTextureType() + "'!");

        // Serialize the texture data
        JsonObject json;
        try{
            //noinspection unchecked
            json = ((TextureType<Object,?>)texture.getTextureType()).serialize(texture.getTextureData());
            if(json == null)
                json = new JsonObject();
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst serializing data for texture type '" + identifier + "'!", e);
        }

        // Add the identifier
        json.addProperty("type", identifier.toString());
        return json;
    }

    public static RawTextureInstance<?,?> deserializeTextureData(JsonObject json){
        if(!finalized)
            throw new RuntimeException("Can only deserialize texture data after registration has completed!");
        //noinspection rawtypes,unchecked
        TextureType<Object,?> textureType = (TextureType)DefaultTextureTypes.BASE;
        Identifier identifier = getIdentifier(textureType);
        if(json.has("type")){
            JsonElement typeJson = json.getAsJsonObject().get("type");
            if(typeJson == null || !typeJson.isJsonPrimitive() || !typeJson.getAsJsonPrimitive().isString())
                throw new JsonParseException("Fusion texture must have string property 'type'!");
            if(!IdentifierUtil.isValidIdentifier(typeJson.getAsString()))
                throw new JsonParseException("Property 'type' must be a valid identifier!");
            identifier = IdentifierUtil.withFusionNamespace(typeJson.getAsString());
            //noinspection unchecked
            textureType = (TextureType<Object,?>)IDENTIFIER_TO_TEXTURE_TYPE.get(identifier);
            if(textureType == null)
                throw new JsonParseException("Unknown texture type '" + identifier + "'!");
        }

        // Deserialize the texture data
        Object textureData;
        try{
            textureData = textureType.deserialize(json);
        }catch(JsonParseException e){
            throw new JsonParseException("Failed to deserialize texture type '" + identifier + "'!", e);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst deserializing data for texture type '" + identifier + "'!", e);
        }
        return RawTextureInstance.of(textureType, textureData);
    }

    public static Identifier getIdentifier(TextureType<?,?> textureType){
        return TEXTURE_TYPE_TO_IDENTIFIER.get(textureType);
    }

    public static void finalizeRegistration(){
        if(!finalized){
            synchronized(TextureTypeRegistryImpl.class){
                finalized = true;
            }
        }
    }
}
