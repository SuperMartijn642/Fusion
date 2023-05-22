package com.supermartijn642.fusion.api.predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.predicate.PredicateRegistryImpl;
import net.minecraft.util.ResourceLocation;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public final class FusionPredicateRegistry {

    /**
     * Registers a new connection predicate type.
     * @param identifier identifier for the predicate type
     * @param serializer serializer used to save the predicates to and load the predicates from json
     * @see ConnectionPredicate
     */
    public static void registerConnectionPredicate(ResourceLocation identifier, Serializer<? extends ConnectionPredicate> serializer){
        PredicateRegistryImpl.registerConnectionPredicate(identifier, serializer);
    }

    /**
     * Serializes the given predicate.
     */
    public static JsonObject serializeConnectionPredicate(ConnectionPredicate predicate){
        return PredicateRegistryImpl.serializeConnectionPredicate(predicate);
    }

    /**
     * Loads a connection predicate from json.
     * @throws JsonParseException if the given json does not match the expected format
     */
    public static ConnectionPredicate deserializeConnectionPredicate(JsonObject json) throws JsonParseException{
        return PredicateRegistryImpl.deserializeConnectionPredicate(json);
    }
}
