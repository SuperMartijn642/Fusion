package com.supermartijn642.fusion.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public class SubModelPart extends ModelPart {

    protected FusionModelPart mainPart;
    private Map<String,SubModelPart> importantChildren = new HashMap<>();
    private boolean finished = false;

    private boolean isMirroringDummy = false;
    private SubModelPart dummyChild;

    public SubModelPart(FusionModelPart mainPart){
        super(List.of(), new HashMap<>());
        this.mainPart = mainPart;
    }

    public void validateModelHasImportantChildren(ModelPart model, Consumer<String> missingPartOutput){
        if(!this.finished)
            throw new IllegalStateException("Model must be finished before validation!");
        for(String importantChild : this.importantChildren.keySet()){
            if(!model.hasChild(importantChild)){
                missingPartOutput.accept(importantChild);
                continue;
            }
            this.importantChildren.get(importantChild).validateModelHasImportantChildren(model.getChild(importantChild), missingPartOutput);
        }
    }

    public void finish(){
        this.finished = true;
        this.importantChildren = Map.copyOf(this.importantChildren);
        for(SubModelPart part : this.importantChildren.values())
            part.finish();
    }

    public void mirror(ModelPart target){
        // Copy properties from target
        this.x = target.x;
        this.y = target.y;
        this.z = target.z;
        this.xRot = target.xRot;
        this.yRot = target.yRot;
        this.zRot = target.zRot;
        this.xScale = target.xScale;
        this.yScale = target.yScale;
        this.zScale = target.zScale;
        this.visible = target.visible;
        this.skipDraw = target.skipDraw;
        this.cubes = target.cubes;
        this.initialPose = target.initialPose;

        // Clear current children
        this.children.clear();

        // Handle dummy targets
        if(target instanceof DummyModelPart){
            if(this.dummyChild == null){
                this.dummyChild = new SubModelPart(this.mainPart);
                this.dummyChild.finished = true;
                this.dummyChild.importantChildren = this.importantChildren;
            }
            this.isMirroringDummy = true;
            this.dummyChild.mirror(((DummyModelPart)target).getDummyChild());
            this.children.put("dummy", this.dummyChild);
            return;
        }
        this.isMirroringDummy = false;

        // Copy children from target
        for(String key : target.children.keySet()){
            if(!this.importantChildren.containsKey(key)){
                this.children.put(key, target.getChild(key));
                continue;
            }
            SubModelPart importantChild = this.importantChildren.get(key);
            this.children.put(key, importantChild);
            importantChild.mirror(target.children.get(key));
        }
    }

    @Override
    public boolean hasChild(String name){
        if(this.isMirroringDummy)
            this.dummyChild.hasChild(name);
        return !this.finished || this.importantChildren.containsKey(name);
    }

    @Override
    public ModelPart getChild(String name){
        if(this.isMirroringDummy)
            this.dummyChild.getChild(name);
        if(this.importantChildren.containsKey(name))
            return this.finished ? this.children.get(name) : this.importantChildren.get(name);
        if(!this.finished){
            SubModelPart part = new SubModelPart(this.mainPart);
            this.importantChildren.put(name, part);
            return part;
        }
        throw new NoSuchElementException("Can't find part " + name);
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k){
        this.mainPart.renderPart(this, poseStack, vertexConsumer, i, j, k);
    }

    protected void renderInternal(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k){
        if(this.visible){
            if(!this.cubes.isEmpty() || !this.children.isEmpty()){
                poseStack.pushPose();
                this.translateAndRotate(poseStack);
                if(!this.skipDraw){
                    for(ModelPart.Cube cube : this.cubes)
                        cube.compile(poseStack.last(), vertexConsumer, i, j, k);
                }

                for(ModelPart child : this.children.values()){
                    if(child instanceof SubModelPart)
                        ((SubModelPart)child).renderInternal(poseStack, vertexConsumer, i, j, k);
                    else
                        child.render(poseStack, vertexConsumer, i, j, k);
                }

                poseStack.popPose();
            }
        }
    }
}
