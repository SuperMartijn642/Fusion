package com.supermartijn642.fusion.model.predicates;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.model.predicates.blockstate.BlockStateModelPredicateRegistryImpl;
import com.supermartijn642.fusion.model.predicates.item.ItemModelPredicateRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public class ModelPredicateRegistryImpl {

    private static final Map<ResourceLocation,Serializer<? extends ModelPredicate>> IDENTIFIER_TO_SERIALIZER = new HashMap<>();
    private static final Map<Serializer<? extends ModelPredicate>,ResourceLocation> SERIALIZER_TO_IDENTIFIER = new HashMap<>();
    private static boolean finalized = false;

    public static synchronized void registerPredicate(ResourceLocation identifier, Serializer<? extends ModelPredicate> serializer){
        if(finalized)
            throw new RuntimeException("Model predicates must be registered before models get loaded!");
        if(IDENTIFIER_TO_SERIALIZER.containsKey(identifier))
            throw new RuntimeException("Duplicate model predicate registration for identifier '" + identifier + "'!");
        if(SERIALIZER_TO_IDENTIFIER.containsKey(serializer))
            throw new RuntimeException("Model predicate has already been registered!");

        IDENTIFIER_TO_SERIALIZER.put(identifier, serializer);
        SERIALIZER_TO_IDENTIFIER.put(serializer, identifier);
    }

    public static JsonObject serializePredicate(ModelPredicate predicate){
        if(!finalized)
            throw new RuntimeException("Can only serialize model predicates after registration has completed!");

        // Handler wrappers
        if(predicate instanceof BlockStateWrapperModelPredicate)
            return FusionBlockStateModelPredicateRegistry.serializeBlockStateModelPredicate(((BlockStateWrapperModelPredicate)predicate).getPredicate());
        else if(predicate instanceof ItemWrapperModelPredicate)
            return FusionItemModelPredicateRegistry.serializeItemModelPredicate(((ItemWrapperModelPredicate)predicate).getPredicate());

        ResourceLocation identifier = SERIALIZER_TO_IDENTIFIER.get(predicate.getSerializer());
        if(identifier == null)
            throw new RuntimeException("Cannot use unregistered model predicate serializer '" + predicate.getSerializer() + "'!");

        // Serialize the model predicate
        JsonObject json;
        try{
            //noinspection unchecked,rawtypes
            json = ((Serializer)predicate.getSerializer()).serialize(predicate);
            if(json == null)
                json = new JsonObject();
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst serializing data for model predicate type '" + identifier + "'!", e);
        }

        // Add the identifier
        json.addProperty("type", identifier.toString());
        return json;
    }

    public static ModelPredicate deserializePredicate(JsonObject json){
        if(!finalized)
            throw new RuntimeException("Can only deserialize model predicates after registration has completed!");
        JsonElement typeJson = json.getAsJsonObject().get("type");
        if(typeJson == null || !typeJson.isJsonPrimitive() || !typeJson.getAsJsonPrimitive().isString())
            throw new JsonParseException("Model predicate must have string property 'type'!");
        if(!IdentifierUtil.isValidIdentifier(typeJson.getAsString()))
            throw new JsonParseException("Property 'type' must be a valid identifier!");
        ResourceLocation identifier = IdentifierUtil.withFusionNamespace(typeJson.getAsString());
        Serializer<? extends ModelPredicate> serializer = IDENTIFIER_TO_SERIALIZER.get(identifier);
        if(serializer == null){
            // Check block state and item model predicate registries
            if(BlockStateModelPredicateRegistryImpl.containsIdentifier(identifier))
                return BlockStateWrapperModelPredicate.create(FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(json));
            else if(ItemModelPredicateRegistryImpl.containsIdentifier(identifier))
                return ItemWrapperModelPredicate.create(FusionItemModelPredicateRegistry.deserializeItemModelPredicate(json));

            throw new JsonParseException("Unknown model predicate type '" + identifier + "'!");
        }

        // Deserialize the model predicate
        ModelPredicate predicate;
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
