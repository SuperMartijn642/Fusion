package com.supermartijn642.fusion.api.predicate;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A predicate used to determine whether a model should connect to with another block.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ConnectionPredicate {

    /**
     * Determines whether the model should connect to the given direction.
     * @param side         side of the block which the relevant texture is on
     * @param ownState     state of the block itself
     * @param otherState   state of the block in the connection direction
     * @param blockInFront state in front of {@code otherstate}
     * @param direction    direction to check
     * @return {@code true} if the texture should connect in the given direction
     */
    boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction);

    /**
     * @return the serializer for this predicate
     */
    Serializer<? extends ConnectionPredicate> getSerializer();

    /**
     * Adds a requirement to this predicate.
     */
    default ConnectionPredicate and(ConnectionPredicate... predicates){
        ConnectionPredicate[] allPredicates = new ConnectionPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultConnectionPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    default ConnectionPredicate or(ConnectionPredicate... predicates){
        ConnectionPredicate[] allPredicates = new ConnectionPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultConnectionPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this resource condition.
     */
    default ConnectionPredicate negate(){
        return DefaultConnectionPredicates.not(this);
    }
}
