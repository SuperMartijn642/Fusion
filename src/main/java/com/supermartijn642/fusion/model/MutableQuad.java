package com.supermartijn642.fusion.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import org.joml.Vector3fc;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private MutableQuadView quadView = null;

    public void set(MutableQuadView quadView){
        this.quadView = quadView;
    }

    public float u(int vertexIndex){
        return this.quadView.u(vertexIndex);
    }

    public float v(int vertexIndex){
        return this.quadView.v(vertexIndex);
    }

    public void uv(int vertexIndex, float u, float v){
        this.quadView.uv(vertexIndex, u, v);
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

    public Vector3fc faceNormal(){
        return this.quadView.faceNormal();
    }
}
