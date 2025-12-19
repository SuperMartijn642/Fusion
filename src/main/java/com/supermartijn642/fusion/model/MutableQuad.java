package com.supermartijn642.fusion.model;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private final Vector3f[] positions = new Vector3f[4];
    private final long[] uvs = new long[4];
    private int tintIndex;
    private Direction lightFace;
    private TextureAtlasSprite sprite;
    private boolean shade;
    private int lightEmission;
    private boolean hasAmbientOcclusion;
    private boolean emissive = false;

    public MutableQuad(){
        for(int i = 0; i < 4; i++)
            this.positions[i] = new Vector3f();
    }

    public void fillFromBakedQuad(BakedQuad quad){
        for(int i = 0; i < 4; i++){
            this.positions[i].set(quad.position(i));
            this.uvs[i] = quad.packedUV(i);
        }
        this.tintIndex = quad.tintIndex();
        this.lightFace = quad.direction();
        this.sprite = quad.sprite();
        this.shade = quad.shade();
        this.lightEmission = quad.lightEmission();
        this.hasAmbientOcclusion = quad.ambientOcclusion();
        this.emissive = false;
    }

    public void emissive(boolean emissive){
        this.emissive = emissive;
    }

    public void ambientOcclusion(boolean ambientOcclusion){
        this.hasAmbientOcclusion = ambientOcclusion;
    }

    public void uv(int vertexIndex, float u, float v){
        this.uvs[vertexIndex] = UVPair.pack(u, v);
    }

    public float u(int vertexIndex){
        return UVPair.unpackU(this.uvs[vertexIndex]);
    }

    public float v(int vertexIndex){
        return UVPair.unpackV(this.uvs[vertexIndex]);
    }

    public void pos(int vertexIndex, float x, float y, float z){
        this.positions[vertexIndex].set(x, y, z);
    }

    public float x(int vertexIndex){
        return this.positions[vertexIndex].x();
    }

    public float y(int vertexIndex){
        return this.positions[vertexIndex].y();
    }

    public float z(int vertexIndex){
        return this.positions[vertexIndex].z();
    }

    public BakedQuad toBakedQuad(){
        return new BakedQuad(
            new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
            this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
            this.tintIndex,
            this.lightFace,
            this.sprite,
            this.shade,
            this.emissive ? 15 : this.lightEmission,
            this.hasAmbientOcclusion
        );
    }
}
