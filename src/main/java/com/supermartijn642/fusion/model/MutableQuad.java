package com.supermartijn642.fusion.model;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private final Vector3f[] positions = new Vector3f[4];
    private final long[] uvs = new long[4];
    private Direction lightFace;
    private BakedQuad.MaterialInfo oldMaterialInfo;
    private boolean hasAmbientOcclusion;
    private BakedNormals bakedNormals = BakedNormals.UNSPECIFIED;
    private BakedColors bakedColors = BakedColors.DEFAULT;
    private boolean emissive = false;
    private int tintIndex;
    private boolean changedMaterialInfo;

    public MutableQuad(){
        for(int i = 0; i < 4; i++)
            this.positions[i] = new Vector3f();
    }

    public void fillFromBakedQuad(BakedQuad quad){
        for(int i = 0; i < 4; i++){
            this.positions[i].set(quad.position(i));
            this.uvs[i] = quad.packedUV(i);
        }
        this.lightFace = quad.direction();
        this.oldMaterialInfo = quad.materialInfo();
        this.hasAmbientOcclusion = quad.materialInfo().ambientOcclusion();
        this.bakedNormals = quad.bakedNormals();
        this.bakedColors = quad.bakedColors();
        this.emissive = false;
        this.tintIndex = quad.materialInfo().tintIndex();
    }

    public void emissive(boolean emissive){
        this.emissive = emissive;
        this.changedMaterialInfo = true;
    }

    public void ambientOcclusion(boolean ambientOcclusion){
        this.hasAmbientOcclusion = ambientOcclusion;
        this.changedMaterialInfo = true;
    }

    public void tintIndex(int tintIndex){
        this.tintIndex = tintIndex;
        this.changedMaterialInfo = true;
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

    public BakedQuad toBakedQuad(ModelBaker.@Nullable Interner interner){
        BakedQuad.MaterialInfo materialInfo;
        if(this.changedMaterialInfo){
            materialInfo = new BakedQuad.MaterialInfo(
                    this.oldMaterialInfo.sprite(),
                    this.oldMaterialInfo.layer(),
                    this.oldMaterialInfo.itemRenderType(),
                    this.tintIndex,
                    this.oldMaterialInfo.shade(),
                    this.emissive ? 15 : this.oldMaterialInfo.lightEmission(),
                    this.hasAmbientOcclusion
            );
            materialInfo = interner != null ? interner.materialInfo(materialInfo) : materialInfo;
        }else{
            materialInfo = this.oldMaterialInfo;
        }
        return new BakedQuad(
            new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
            this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
            this.lightFace,
            materialInfo,
            this.bakedNormals,
            this.bakedColors
        );
    }
}
