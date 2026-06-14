package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 13/05/2026 by SuperMartijn642
 */
public class EmittableQuadImpl extends MutableQuadImpl implements EmittableQuad {

    public static EmittableQuad create(Consumer<MutableQuad> emitter){
        return new EmittableQuadImpl(emitter);
    }

    private final Consumer<MutableQuad> emitter;
    private final List<Transform> transforms = new ArrayList<>(4);
    private final List<MutableQuad> snapshots = new ArrayList<>(4);

    private EmittableQuadImpl(Consumer<MutableQuad> emitter){
        this.emitter = emitter;
    }

    @Override
    public Popper pushTransform(Transform transform){
        this.transforms.add(transform);
        int index = this.transforms.size();
        return new Popper() {
            private boolean closed = false;

            @Override
            public void popTransform(){
                if(this.closed)
                    return;
                if(EmittableQuadImpl.this.transforms.size() > index)
                    throw new IllegalStateException("A newer transform has not been closed!");
                if(EmittableQuadImpl.this.transforms.size() < index)
                    throw new AssertionError();
                this.closed = true;
                EmittableQuadImpl.this.transforms.removeLast();
            }
        };
    }

    @Override
    public void emit(){
        if(this.transforms.isEmpty()){
            this.emitter.accept(this);
            return;
        }

        // Create snapshot of current state
        while(this.snapshots.size() < this.transforms.size())
            this.snapshots.add(MutableQuad.create());
        MutableQuad snapshot = this.snapshots.get(this.transforms.size() - 1);
        snapshot.copyFrom(this);

        // Apply transforms
        Transform transform = this.transforms.removeLast();
        int index = this.transforms.size();
        try{
            transform.transform(this);
        }catch(Exception e){
            while(this.transforms.size() > index)
                this.transforms.removeLast();
            this.transforms.add(transform);
            this.copyFrom(snapshot);
            throw e;
        }
        if(this.transforms.size() > index)
            throw new IllegalStateException("A transform has not been closed!");
        if(this.transforms.size() < index)
            throw new AssertionError();
        this.transforms.add(transform);
        this.copyFrom(snapshot);
    }
}
