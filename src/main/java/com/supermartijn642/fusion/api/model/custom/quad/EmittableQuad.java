package com.supermartijn642.fusion.api.model.custom.quad;

import com.supermartijn642.fusion.model.custom.quad.EmittableQuadImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * A {@link MutableQuad} that can be emitted.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 * @see MutableQuad
 */
@ApiStatus.NonExtendable
public interface EmittableQuad extends MutableQuad {

    /**
     * Creates a mutable quad with the given consumer as implementation for {@link #emit()}.
     */
    static EmittableQuad create(Consumer<MutableQuad> emitter){
        return EmittableQuadImpl.create(emitter);
    }

    /**
     * Pushes a transform that is applied as an in-between step when quads are emitted.
     */
    Popper pushTransform(Transform transform);

    /**
     * Emit the current state of the quad.
     */
    void emit();

    @FunctionalInterface
    interface Transform {
        /**
         * Transforms the given quad.
         * Can emit none or multiple quads.
         */
        void transform(EmittableQuad quad);
    }

    @ApiStatus.NonExtendable
    interface Popper extends AutoCloseable {
        /**
         * Pops the pushed transform.
         */
        void popTransform();

        @Override
        default void close(){ // No exceptions
            this.popTransform();
        }
    }
}
