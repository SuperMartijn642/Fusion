package com.supermartijn642.fusion.model.predicates.blockstate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class BlockStateModelPredicateRegistryImpl {

    private static final Map<ResourceLocation,Serializer<? extends BlockStateModelPredicate>> IDENTIFIER_TO_SERIALIZER = new HashMap<>();
    private static final Map<Serializer<? extends BlockStateModelPredicate>,ResourceLocation> SERIALIZER_TO_IDENTIFIER = new HashMap<>();
    private static boolean finalized = false;

    public static synchronized void registerPredicate(ResourceLocation identifier, Serializer<? extends BlockStateModelPredicate> serializer){
        if(finalized)
            throw new RuntimeException("Block state model predicates must be registered before models get loaded!");
        if(IDENTIFIER_TO_SERIALIZER.containsKey(identifier))
            throw new RuntimeException("Duplicate block state model predicate registration for identifier '" + identifier + "'!");
        if(SERIALIZER_TO_IDENTIFIER.containsKey(serializer))
            throw new RuntimeException("Block state model predicate has already been registered!");

        IDENTIFIER_TO_SERIALIZER.put(identifier, serializer);
        SERIALIZER_TO_IDENTIFIER.put(serializer, identifier);
    }

    public static synchronized boolean containsIdentifier(ResourceLocation identifier){
        return IDENTIFIER_TO_SERIALIZER.containsKey(identifier);
    }

    public static JsonObject serializePredicate(BlockStateModelPredicate predicate){
        if(!finalized)
            throw new RuntimeException("Can only serialize block state model predicates after registration has completed!");
        ResourceLocation identifier = SERIALIZER_TO_IDENTIFIER.get(predicate.getSerializer());
        if(identifier == null)
            throw new RuntimeException("Cannot use unregistered block state model predicate serializer '" + predicate.getSerializer() + "'!");

        // Serialize the block state model predicate
        JsonObject json;
        try{
            //noinspection unchecked,rawtypes
            json = ((Serializer)predicate.getSerializer()).serialize(predicate);
            if(json == null)
                json = new JsonObject();
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst serializing data for block state model predicate type '" + identifier + "'!", e);
        }

        // Add the identifier
        json.addProperty("type", identifier.toString());
        return json;
    }

    public static BlockStateModelPredicate deserializePredicate(JsonObject json){
        if(!finalized)
            throw new RuntimeException("Can only deserialize block state model predicates after registration has completed!");
        JsonElement typeJson = json.getAsJsonObject().get("type");
        if(typeJson == null || !typeJson.isJsonPrimitive() || !typeJson.getAsJsonPrimitive().isString())
            throw new JsonParseException("Block state model predicate must have string property 'type'!");
        if(!IdentifierUtil.isValidIdentifier(typeJson.getAsString()))
            throw new JsonParseException("Property 'type' must be a valid identifier!");
        ResourceLocation identifier = IdentifierUtil.withFusionNamespace(typeJson.getAsString());
        Serializer<? extends BlockStateModelPredicate> serializer = IDENTIFIER_TO_SERIALIZER.get(identifier);
        if(serializer == null)
            throw new JsonParseException("Unknown block state model predicate type '" + identifier + "'!");

        // Deserialize the block state model predicate
        BlockStateModelPredicate predicate;
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
        finalized = true;
    }
}
