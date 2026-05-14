package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A predicate for block state models.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public interface BlockStateModelPredicate extends ModelPredicate {

    boolean test(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state);
}
