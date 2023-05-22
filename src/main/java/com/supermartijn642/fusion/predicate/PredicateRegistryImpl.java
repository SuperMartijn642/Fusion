package com.supermartijn642.fusion.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class PredicateRegistryImpl {

    private static final Map<ResourceLocation,Serializer<? extends ConnectionPredicate>> IDENTIFIER_TO_SERIALIZER = new HashMap<>();
    private static final Map<Serializer<? extends ConnectionPredicate>,ResourceLocation> SERIALIZER_TO_IDENTIFIER = new HashMap<>();
    private static boolean finalized = false;

    public static synchronized void registerConnectionPredicate(ResourceLocation identifier, Serializer<? extends ConnectionPredicate> serializer){
        if(finalized)
            throw new RuntimeException("Predicates must be registered before models get loaded!");
        if(IDENTIFIER_TO_SERIALIZER.containsKey(identifier))
            throw new RuntimeException("Duplicate predicate registration for identifier '" + identifier + "'!");
        if(SERIALIZER_TO_IDENTIFIER.containsKey(serializer))
            throw new RuntimeException("Predicate has already been registered!");

        IDENTIFIER_TO_SERIALIZER.put(identifier, serializer);
        SERIALIZER_TO_IDENTIFIER.put(serializer, identifier);
    }

    public static JsonObject serializeConnectionPredicate(ConnectionPredicate predicate){
        if(!finalized)
            throw new RuntimeException("Can only serialize predicates after registration has completed!");
        ResourceLocation identifier = SERIALIZER_TO_IDENTIFIER.get(predicate.getSerializer());
        if(identifier == null)
            throw new RuntimeException("Cannot use unregistered predicate serializer '" + predicate.getSerializer() + "'!");

        // Serialize the predicate
        JsonObject json;
        try{
            //noinspection unchecked,rawtypes
            json = ((Serializer)predicate.getSerializer()).serialize(predicate);
            if(json == null)
                json = new JsonObject();
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst serializing data for predicate type '" + identifier + "'!", e);
        }

        // Add the identifier
        json.addProperty("type", identifier.toString());
        return json;
    }

    public static ConnectionPredicate deserializeConnectionPredicate(JsonObject json){
        if(!finalized)
            throw new RuntimeException("Can only deserialize predicates after registration has completed!");
        JsonElement typeJson = json.getAsJsonObject().get("type");
        if(typeJson == null || !typeJson.isJsonPrimitive() || !typeJson.getAsJsonPrimitive().isString())
            throw new JsonParseException("Predicate must have string property 'type'!");
        if(!IdentifierUtil.isValidIdentifier(typeJson.getAsString()))
            throw new JsonParseException("Property 'type' must be a valid identifier!");
        ResourceLocation identifier = IdentifierUtil.withFusionNamespace(typeJson.getAsString());
        Serializer<? extends ConnectionPredicate> serializer = IDENTIFIER_TO_SERIALIZER.get(identifier);
        if(serializer == null)
            throw new JsonParseException("Unknown predicate type '" + identifier + "'!");

        // Deserialize the predicate
        ConnectionPredicate predicate;
        try{
            predicate = serializer.deserialize(json);
        }catch(JsonParseException e){
            throw new JsonParseException("Invalid json for predicate type '" + identifier + "'!", e);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst deserializing data for predicate type '" + identifier + "'!", e);
        }
        return predicate;
    }

    public static void finalizeRegistration(){
        if(!finalized){
            synchronized(PredicateRegistryImpl.class){
                finalized = true;
            }
        }
    }
}
