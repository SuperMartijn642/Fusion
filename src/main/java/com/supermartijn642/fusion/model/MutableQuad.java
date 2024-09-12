package com.supermartijn642.fusion.model;

import com.mojang.math.Vector3f;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private MutableQuadView quadView = null;

    public void set(MutableQuadView quadView){
        this.quadView = quadView;
    }

    public float u(int vertexIndex){
        return this.quadView.spriteU(vertexIndex, 0);
    }

    public float v(int vertexIndex){
        return this.quadView.spriteV(vertexIndex, 0);
    }

    public void uv(int vertexIndex, float u, float v){
        this.quadView.sprite(vertexIndex, 0, u, v);
    }

    public float x(int vertexIndex){
        return this.quadView.x(vertexIndex);
    }

    public float y(int vertexIndex){
        return this.quadView.y(vertexIndex);
    }

    public float z(int vertexIndex){
        return this.quadView.z(vertexIndex);
    }

    public void pos(int vertexIndex, float x, float y, float z){
        this.quadView.pos(vertexIndex, x, y, z);
    }

    public Vector3f faceNormal(){
        return this.quadView.faceNormal();
    }
}
