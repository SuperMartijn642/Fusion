package com.supermartijn642.fusion.model.types.connecting;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import org.joml.Vector3f;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private MutableQuadView quadView = null;
    private int[] permutation = null;

    public void set(MutableQuadView quadView, int[] permutation){
        this.quadView = quadView;
        this.permutation = permutation;
    }

    public float u(int vertexIndex){
        return this.quadView.u(this.permutation[vertexIndex]);
    }

    public float v(int vertexIndex){
        return this.quadView.v(this.permutation[vertexIndex]);
    }

    public void uv(int vertexIndex, float u, float v){
        this.quadView.uv(this.permutation[vertexIndex], u, v);
    }

    public float x(int vertexIndex){
        return this.quadView.x(this.permutation[vertexIndex]);
    }

    public float y(int vertexIndex){
        return this.quadView.y(this.permutation[vertexIndex]);
    }

    public float z(int vertexIndex){
        return this.quadView.z(this.permutation[vertexIndex]);
    }

    public void pos(int vertexIndex, float x, float y, float z){
        this.quadView.pos(this.permutation[vertexIndex], x, y, z);
    }

    public Vector3f faceNormal(){
        return this.quadView.faceNormal();
    }
}
