package com.supermartijn642.fusion.api.model.predicates.blockstate;

import com.supermartijn642.fusion.api.util.Serializer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A predicate for block state models.
 * <p>
 * Created 14/05/2026 by SuperMartijn642
 */
public interface BlockStateModelPredicate {

    boolean test(@Nullable IEnviromentBlockReader level, @Nullable BlockPos pos, @Nullable BlockState state);

    /**
     * Simplifies the predicate. May be used to simplify user properties.
     * For example, an and-predicate may flatten nested and-predicates or an empty or-predicate may return a false-predicate.
     */
    default BlockStateModelPredicate simplify(){
        return this;
    }

    /**
     * Serializer for this predicate.
     */
    Serializer<? extends BlockStateModelPredicate> getSerializer();

    /**
     * Checks whether this predicate is {@link DefaultBlockStateModelPredicates#always()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysTrue(){
        return this == DefaultBlockStateModelPredicates.always();
    }

    /**
     * Checks whether this predicate is {@link DefaultBlockStateModelPredicates#never()}.
     */
    @ApiStatus.NonExtendable
    default boolean alwaysFalse(){
        return this == DefaultBlockStateModelPredicates.never();
    }

    /**
     * Adds a requirement to this predicate.
     */
    @ApiStatus.NonExtendable
    default BlockStateModelPredicate and(BlockStateModelPredicate... predicates){
        BlockStateModelPredicate[] allPredicates = new BlockStateModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultBlockStateModelPredicates.and(allPredicates);
    }

    /**
     * Adds an alternative to this predicate.
     */
    @ApiStatus.NonExtendable
    default BlockStateModelPredicate or(BlockStateModelPredicate... predicates){
        BlockStateModelPredicate[] allPredicates = new BlockStateModelPredicate[predicates.length + 1];
        allPredicates[0] = this;
        System.arraycopy(predicates, 0, allPredicates, 1, predicates.length);
        return DefaultBlockStateModelPredicates.or(allPredicates);
    }

    /**
     * Negates the output of this resource condition.
     */
    @ApiStatus.NonExtendable
    default BlockStateModelPredicate negate(){
        return DefaultBlockStateModelPredicates.not(this);
    }
}
