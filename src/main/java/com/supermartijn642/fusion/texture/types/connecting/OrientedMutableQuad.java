package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class OrientedMutableQuad extends MutableQuadImpl {

    private static final int[] DEFAULT_PERMUTATION = {0, 1, 2, 3};

    private int[] permutation = DEFAULT_PERMUTATION;

    public OrientedMutableQuad(){
    }

    public void setPermutation(int[] permutation){
        this.permutation = permutation;
    }

    public void resetPermutation(){
        this.permutation = DEFAULT_PERMUTATION;
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        super.copyFrom(quad);
        // Copy vertex specific data through the overwritten methods
        for(int i = 0; i < 4; i++){
            this.position(i, quad.position(i));
            this.uv(i, quad.u(i), quad.v(i));
        }
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        super.copyBakedQuad(quad);
        // Copy vertex specific data through the overwritten methods
        for(int i = 0; i < 4; i++){
            this.position(i,
                BakedQuadHelper.getPosX(quad.getVertices(), i),
                BakedQuadHelper.getPosY(quad.getVertices(), i),
                BakedQuadHelper.getPosZ(quad.getVertices(), i)
            );
            this.uv(i,
                BakedQuadHelper.getU(quad.getVertices(), i),
                BakedQuadHelper.getV(quad.getVertices(), i)
            );
        }
        return this;
    }

    @Override
    public MutableQuad position(int vertexIndex, float x, float y, float z){
        return super.position(this.permutation[vertexIndex], x, y, z);
    }

    @Override
    public MutableQuad position(int vertexIndex, Vector3f position){
        return super.position(this.permutation[vertexIndex], position);
    }

    @Override
    public Vector3f position(int vertexIndex){
        return super.position(this.permutation[vertexIndex]);
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

    @Override
    public MutableQuad uv(int vertexIndex, float u, float v){
        return super.uv(this.permutation[vertexIndex], u, v);
    }

    @Override
    public float u(int vertexIndex){
        return super.u(this.permutation[vertexIndex]);
    }

    @Override
    public float v(int vertexIndex){
        return super.v(this.permutation[vertexIndex]);
    }
}
