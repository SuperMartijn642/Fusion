package com.supermartijn642.fusion.api.texture.types.connecting.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.texture.types.connecting.predicates.ConnectionPredicateRegistryImpl;
import net.minecraft.util.ResourceLocation;

/**
 * Registry for connection predicates.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 * @see ConnectionPredicate
 */
public final class FusionConnectionPredicateRegistry {

    /**
     * Registers a new connection predicate type.
     * @param identifier identifier for the predicate type
     * @param serializer serializer used to save the predicates to and load the predicates from json
     * @see ConnectionPredicate
     */
    public static void registerConnectionPredicate(ResourceLocation identifier, Serializer<? extends ConnectionPredicate> serializer){
        ConnectionPredicateRegistryImpl.registerPredicate(identifier, serializer);
    }

    /**
     * Serializes the given predicate.
     */
    public static JsonObject serializeConnectionPredicate(ConnectionPredicate predicate){
        return ConnectionPredicateRegistryImpl.serializePredicate(predicate);
    }

    /**
     * Loads a connection predicate from json.
     * @throws JsonParseException if the given json does not match the expected format
     */
    public static ConnectionPredicate deserializeConnectionPredicate(JsonObject json) throws JsonParseException{
        return ConnectionPredicateRegistryImpl.deserializePredicate(json);
    }
}
