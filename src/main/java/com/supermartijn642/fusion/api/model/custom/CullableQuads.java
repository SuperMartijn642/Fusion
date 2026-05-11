package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.model.custom.CullableQuadsImpl;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A collection of quads where each quad is associated with a cull direction.
 * <p>
 * Created 03/05/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface CullableQuads {

    /**
     * Creates a builder for a cullable collection of quads.
     */
    static Builder builder(){
        return CullableQuadsImpl.builder();
    }

    /**
     * Combines the given collections of quads into one.
     */
    static CullableQuads combine(CullableQuads... quads){
        Builder builder = builder();
        for(CullableQuads quad : quads)
            builder.add(quad);
        return builder.build();
    }

    /**
     * An empty cullable quads collection.
     */
    static CullableQuads empty(){
        return builder().build();
    }

    /**
     * Gets the quads for the given cull direction.
     */
    List<QuadAccess> get(@Nullable Direction cullDirection);

    /**
     * All quads no matter their cull direction.
     */
    List<QuadAccess> all();

    /**
     * Quads that should not be culled.
     */
    List<QuadAccess> unculled();

    /**
     * Quads that should be culled when the up direction is covered.
     */
    List<QuadAccess> up();

    /**
     * Quads that should be culled when the down direction is covered.
     */
    List<QuadAccess> down();

    /**
     * Quads that should be culled when the north side is covered.
     */
    List<QuadAccess> north();

    /**
     * Quads that should be culled when the east side is covered.
     */
    List<QuadAccess> east();

    /**
     * Quads that should be culled when the south side is covered.
     */
    List<QuadAccess> south();

    /**
     * Quads that should be culled when the west side is covered.
     */
    List<QuadAccess> west();

    /**
     * Creates a new collection containing this collection's quads mutated by the given mutator.
     * @see QuadMutator#mutate(Direction, MutableQuad)
     */
    default CullableQuads mutateQuads(QuadMutator mutator){
        return builder().add(this).mutateQuads(mutator).build();
    }

    @FunctionalInterface
    interface QuadMutator {

        /**
         * Mutates a given quad.
         * @param cullDirection cull direction for the quad, may be null if unculled
         * @param quad          quad to be mutated
         * @return {@code true} to keep the quad, {@code false} to discard it
         */
        boolean mutate(@Nullable Direction cullDirection, MutableQuad quad);
    }

    @ApiStatus.NonExtendable
    interface Builder {

        /**
         * Adds the given quads for the given cull direction.
         * If no cull direction is given, the quad will not be culled.
         */
        Builder add(@Nullable Direction cullDirection, List<QuadAccess> quads);

        /**
         * Adds the given quad for the given cull direction.
         * If no cull direction is given, the quad will not be culled.
         */
        Builder add(@Nullable Direction cullDirection, QuadAccess quad);

        /**
         * Adds the quads in the given collection.
         */
        default Builder add(CullableQuads quads){
            this.add(null, quads.unculled());
            this.add(Direction.UP, quads.get(Direction.UP));
            this.add(Direction.DOWN, quads.get(Direction.DOWN));
            this.add(Direction.NORTH, quads.get(Direction.NORTH));
            this.add(Direction.EAST, quads.get(Direction.EAST));
            this.add(Direction.SOUTH, quads.get(Direction.SOUTH));
            this.add(Direction.WEST, quads.get(Direction.WEST));
            return this;
        }

        /**
         * Mutates the quads in the builder through the given mutator.
         * @see QuadMutator#mutate(Direction, MutableQuad)
         */
        Builder mutateQuads(QuadMutator mutator);

        /**
         * Builds the cullable quad collection.
         */
        CullableQuads build();
    }
}
