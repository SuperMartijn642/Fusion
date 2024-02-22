package com.supermartijn642.fusion.api.predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * Convenience extension for {@link ConnectionPredicate} which by default makes the connection predicate sensitive.
 * <p>
 * Created 22/02/2024 by SuperMartijn642
 */
public interface SensitiveConnectionPredicate extends ConnectionPredicate {

    default boolean shouldConnect(EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction){
        throw new IllegalStateException("This method should not be called for sensitive connection predicates!");
    }

    boolean shouldConnect(IBlockAccess level, BlockPos pos, EnumFacing side, @Nullable IBlockState ownState, IBlockState otherState, IBlockState blockInFront, ConnectionDirection direction);

    @Override
    default boolean isSensitive(){
        return true;
    }
}
