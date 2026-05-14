package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;

import java.util.function.Consumer;

/**
 * Created 13/05/2026 by SuperMartijn642
 */
public class EmittableQuadImpl extends MutableQuadImpl implements EmittableQuad {

    public static EmittableQuad create(Consumer<MutableQuad> emitter){
        return new EmittableQuadImpl(emitter);
    }

    private final Consumer<MutableQuad> emitter;

    private EmittableQuadImpl(Consumer<MutableQuad> emitter){
        this.emitter = emitter;
    }

    @Override
    public void emit(){
        this.emitter.accept(this);
    }
}
