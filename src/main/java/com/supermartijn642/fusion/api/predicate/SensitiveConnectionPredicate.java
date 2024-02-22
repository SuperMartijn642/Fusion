package com.supermartijn642.fusion.api.predicate;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

/**
 * Convenience extension for {@link ConnectionPredicate} which by default makes the connection predicate sensitive.
 * <p>
 * Created 22/02/2024 by SuperMartijn642
 */
public interface SensitiveConnectionPredicate extends ConnectionPredicate {

    default boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        throw new IllegalStateException("This method should not be called for sensitive connection predicates!");
    }

    boolean shouldConnect(IBlockReader level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction);

    @Override
    default boolean isSensitive(){
        return true;
    }
}
