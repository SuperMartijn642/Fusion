package com.supermartijn642.fusion.api.model.predicates;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.model.predicates.ModelPredicateRegistryImpl;
import net.minecraft.util.ResourceLocation;

/**
 * Registry for model predicates.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 * @see ModelPredicate
 */
public final class FusionModelPredicateRegistry {

    /**
     * Registers a new model predicate type.
     * @param identifier identifier for the predicate type
     * @param serializer serializer used to save the predicates to and load the predicates from json
     * @see ModelPredicate
     */
    public static void registerModelPredicate(ResourceLocation identifier, Serializer<? extends ModelPredicate> serializer){
        ModelPredicateRegistryImpl.registerPredicate(identifier, serializer);
    }

    /**
     * Serializes the given predicate.
     */
    public static JsonObject serializeModelPredicate(ModelPredicate predicate){
        return ModelPredicateRegistryImpl.serializePredicate(predicate);
    }

    /**
     * Loads a model predicate from json.
     * @throws JsonParseException if the given json does not match the expected format
     */
    public static ModelPredicate deserializeModelPredicate(JsonObject json) throws JsonParseException{
        return ModelPredicateRegistryImpl.deserializePredicate(json);
    }
}
