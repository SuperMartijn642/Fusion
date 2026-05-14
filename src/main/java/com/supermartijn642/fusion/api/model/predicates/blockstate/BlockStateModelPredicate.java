package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

/**
 * A predicate for block state models.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public interface BlockStateModelPredicate extends ModelPredicate {

    boolean test(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state);
}
