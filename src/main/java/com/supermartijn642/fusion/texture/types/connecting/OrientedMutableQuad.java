package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3fc;

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
        this.neoBakedNormals(quad.neoBakedNormals());
        this.neoBakedColors(quad.neoBakedColors());
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        super.copyBakedQuad(quad);
        // Copy vertex specific data through the overwritten methods
        for(int i = 0; i < 4; i++){
            this.position(i, quad.position(i));
            long uv = quad.packedUV(i);
            this.uv(i, UVPair.unpackU(uv), UVPair.unpackV(uv));
        }
        this.neoBakedNormals(quad.bakedNormals());
        this.neoBakedColors(quad.bakedColors());
        return this;
    }

    @Override
    public MutableQuad position(int vertexIndex, float x, float y, float z){
        return super.position(this.permutation[vertexIndex], x, y, z);
    }

    @Override
    public MutableQuad position(int vertexIndex, Vector3fc position){
        return super.position(this.permutation[vertexIndex], position);
    }

    @Override
    public Vector3fc position(int vertexIndex){
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

    @Override
    public MutableQuad neoBakedNormals(BakedNormals bakedNormals){
        for(int i = 0; i < 4; i++){
            this.neoNormal(
                this.permutation[i],
                BakedNormals.unpackX(bakedNormals.normal(i)),
                BakedNormals.unpackY(bakedNormals.normal(i)),
                BakedNormals.unpackZ(bakedNormals.normal(i))
            );
        }
        return this;
    }

    @Override
    public MutableQuad neoNormal(int vertexIndex, float x, float y, float z){
        return super.neoNormal(this.permutation[vertexIndex], x, y, z);
    }

    @Override
    public MutableQuad neoNormal(int vertexIndex, Vector3fc position){
        return super.neoNormal(this.permutation[vertexIndex], position);
    }

    @Override
    public BakedNormals neoBakedNormals(){
        return BakedNormals.of(
            BakedNormals.pack(this.neoNormal(this.permutation[0])),
            BakedNormals.pack(this.neoNormal(this.permutation[1])),
            BakedNormals.pack(this.neoNormal(this.permutation[2])),
            BakedNormals.pack(this.neoNormal(this.permutation[3]))
        );
    }

    @Override
    public Vector3fc neoNormal(int vertexIndex){
        return super.neoNormal(this.permutation[vertexIndex]);
    }

    @Override
    public MutableQuad neoBakedColors(BakedColors bakedColors){
        for(int i = 0; i < 4; i++)
            this.neoColor(this.permutation[i], bakedColors.color(i));
        return this;
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, int color){
        return super.neoColor(this.permutation[vertexIndex], color);
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, float r, float g, float b, float a){
        return super.neoColor(this.permutation[vertexIndex], r, g, b, a);
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, float r, float g, float b){
        return super.neoColor(this.permutation[vertexIndex], r, g, b);
    }

    @Override
    public BakedColors neoBakedColors(){
        return BakedColors.of(
            this.neoColor(this.permutation[0]),
            this.neoColor(this.permutation[1]),
            this.neoColor(this.permutation[2]),
            this.neoColor(this.permutation[3])
        );
    }

    @Override
    public int neoColor(int vertexIndex){
        return super.neoColor(this.permutation[vertexIndex]);
    }
}
