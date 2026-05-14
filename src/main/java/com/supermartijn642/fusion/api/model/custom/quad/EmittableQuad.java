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
     * Emit the current state of the quad.
     */
    void emit();
}
