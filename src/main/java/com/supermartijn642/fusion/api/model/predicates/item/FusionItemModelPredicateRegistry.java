package com.supermartijn642.fusion.api.model.predicates.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.model.predicates.item.ItemModelPredicateRegistryImpl;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry for item model predicates.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 * @see ItemModelPredicate
 */
public final class FusionItemModelPredicateRegistry {

    /**
     * Registers a new item model predicate type.
     * @param identifier identifier for the predicate type
     * @param serializer serializer used to save the predicates to and load the predicates from json
     * @see ItemModelPredicate
     */
    public static void registerItemModelPredicate(ResourceLocation identifier, Serializer<? extends ItemModelPredicate> serializer){
        ItemModelPredicateRegistryImpl.registerPredicate(identifier, serializer);
    }

    /**
     * Serializes the given predicate.
     */
    public static JsonObject serializeItemModelPredicate(ItemModelPredicate predicate){
        return ItemModelPredicateRegistryImpl.serializePredicate(predicate);
    }

    /**
     * Loads an item model predicate from json.
     * @throws JsonParseException if the given json does not match the expected format
     */
    public static ItemModelPredicate deserializeItemModelPredicate(JsonObject json) throws JsonParseException{
        return ItemModelPredicateRegistryImpl.deserializePredicate(json);
    }
}
