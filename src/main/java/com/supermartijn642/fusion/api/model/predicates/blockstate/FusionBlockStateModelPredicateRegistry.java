package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.util.Serializer;
import com.supermartijn642.fusion.model.predicates.blockstate.BlockStateModelPredicateRegistryImpl;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry for block state model predicates.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 * @see BlockStateModelPredicate
 */
public final class FusionBlockStateModelPredicateRegistry {

    /**
     * Registers a new block state model predicate type.
     * @param identifier identifier for the predicate type
     * @param serializer serializer used to save the predicates to and load the predicates from json
     * @see BlockStateModelPredicate
     */
    public static void registerBlockStateModelPredicate(ResourceLocation identifier, Serializer<? extends BlockStateModelPredicate> serializer){
        BlockStateModelPredicateRegistryImpl.registerPredicate(identifier, serializer);
    }

    /**
     * Serializes the given predicate.
     */
    public static JsonObject serializeBlockStateModelPredicate(BlockStateModelPredicate predicate){
        return BlockStateModelPredicateRegistryImpl.serializePredicate(predicate);
    }

    /**
     * Loads a block state model predicate from json.
     * @throws JsonParseException if the given json does not match the expected format
     */
    public static BlockStateModelPredicate deserializeBlockStateModelPredicate(JsonObject json) throws JsonParseException{
        return BlockStateModelPredicateRegistryImpl.deserializePredicate(json);
    }
}
