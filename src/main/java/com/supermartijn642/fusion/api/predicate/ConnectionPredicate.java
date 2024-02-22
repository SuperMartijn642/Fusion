package com.supermartijn642.fusion.api.predicate;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

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
     * @throws IllegalStateException when the predicate is sensitive, i.e. {@link #isSensitive()} returns {@code true}
     */
    boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction);

    /**
     * Determines whether the model should connect to the given direction. Sensitive version of {@link #shouldConnect(EnumFacing, IBlockState, IBlockState, IBlockState, ConnectionDirection)}.
     * If this method should be used, {@link #isSensitive()} must return {@code true}.
     * @param side         side of the block which the relevant texture is on
     * @param ownState     state of the block itself
     * @param otherState   state of the block in the connection direction
     * @param blockInFront state in front of {@code otherstate}
     * @param direction    direction to check
     * @return {@code true} if the texture should connect in the given direction
     */
    default boolean shouldConnect(IBlockAccess level, BlockPos pos, EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        return this.shouldConnect(side, ownState, otherState, blockInFront, direction);
    }

    /**
     * Determines whether this connection predicate depends on the level and should always be reevaluated.
     * If {@code true} is returned {@link #shouldConnect(EnumFacing, IBlockState, IBlockState, IBlockState, ConnectionDirection)} may throw an {@link IllegalStateException}.
     * If {@code false} is returned, it is assumed that the connection predicate may be cached for the values supplied in {@link #shouldConnect(EnumFacing, IBlockState, IBlockState, IBlockState, ConnectionDirection)}.
     * @see SensitiveConnectionPredicate
     */
    default boolean isSensitive(){
        return false;
    }

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
