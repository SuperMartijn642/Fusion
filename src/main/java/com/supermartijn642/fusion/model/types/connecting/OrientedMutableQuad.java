package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.model.MutableQuad;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class OrientedMutableQuad extends MutableQuad {

    private static final int[] DEFAULT_PERMUTATION = {0, 1, 2, 3};

    private int[] permutation = DEFAULT_PERMUTATION;

    public OrientedMutableQuad(){
    }

    public void set(int[] permutation){
        this.permutation = permutation;
    }

    public void resetPermutation(){
        this.permutation = DEFAULT_PERMUTATION;
    }

    @Override
    public void lightmap(int vertexIndex, int lightmap){
        super.lightmap(this.permutation[vertexIndex], lightmap);
    }

    @Override
    public int lightmap(int vertexIndex){
        return super.lightmap(this.permutation[vertexIndex]);
    }

    @Override
    public void uv(int vertexIndex, float u, float v){
        super.uv(this.permutation[vertexIndex], u, v);
    }

    @Override
    public float u(int vertexIndex){
        return super.u(this.permutation[vertexIndex]);
    }

    @Override
    public float v(int vertexIndex){
        return super.v(this.permutation[vertexIndex]);
    }

    @Override
    public void pos(int vertexIndex, float x, float y, float z){
        super.pos(this.permutation[vertexIndex], x, y, z);
    }

    @Override
    public float x(int vertexIndex){
        return super.x(this.permutation[vertexIndex]);
    }

    @Override
    public float y(int vertexIndex){
        return super.y(this.permutation[vertexIndex]);
    }

    @Override
    public float z(int vertexIndex){
        return super.z(this.permutation[vertexIndex]);
    }
}
