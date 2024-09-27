package com.supermartijn642.fusion.entity.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;
import java.util.Map;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public class DummyModelPart extends ModelPart {

    protected ModelPart child;

    public DummyModelPart(ModelPart childPart){
        super(List.of(), Map.of("dummy", childPart));
        this.child = childPart;
    }

    public void setDummyChild(ModelPart child){
        this.child = child;
        this.children = Map.of("dummy", child);
    }

    @Override
    public boolean hasChild(String name){
        return this.child.hasChild(name);
    }

    @Override
    public ModelPart getChild(String name){
        return this.child.getChild(name);
    }

    public ModelPart getProperChild(){
        return this.child instanceof DummyModelPart ? ((DummyModelPart)this.child).getProperChild() : this.child;
    }

    public ModelPart getDummyChild(){
        return this.child;
    }
}
