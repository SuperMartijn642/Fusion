package com.supermartijn642.fusion.api.predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Convenience extension for {@link ConnectionPredicate} which by default makes the connection predicate sensitive.
 * <p>
 * Created 22/02/2024 by SuperMartijn642
 */
public interface SensitiveConnectionPredicate extends ConnectionPredicate {

    default boolean shouldConnect(Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction){
        throw new IllegalStateException("This method should not be called for sensitive connection predicates!");
    }

    boolean shouldConnect(BlockGetter level, BlockPos pos, Direction side, @Nullable BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction);

    @Override
    default boolean isSensitive(){
        return true;
    }
}
