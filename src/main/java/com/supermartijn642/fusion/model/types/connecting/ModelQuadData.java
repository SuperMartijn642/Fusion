package com.supermartijn642.fusion.model.types.connecting;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;

/**
 * Created 12/11/2023 by SuperMartijn642
 */
public class ModelQuadData {

    public static ModelQuadData copyOf(QuadView quad){
        return new ModelQuadData(quad);
    }

    private final Direction cullFace;
    private final Direction nominalFace;
    private final int colorIndex;
    private final RenderMaterial material;
    private final int tag;
    private final float[][] vertexPos = new float[4][3];
    private final int[] vertexColor = new int[4];
    private final float[][] vertexUV = new float[4][2];
    private final int[] vertexLight = new int[4];
    private final float[][] vertexNormal = new float[4][3];


    private ModelQuadData(QuadView quad){
        this.cullFace = quad.cullFace();
        this.nominalFace = quad.nominalFace();
        this.colorIndex = quad.colorIndex();
        this.material = quad.material();
        this.tag = quad.tag();
        for(int i = 0; i < 4; i++){
            this.vertexPos[i][0] = quad.x(i);
            this.vertexPos[i][1] = quad.y(i);
            this.vertexPos[i][2] = quad.z(i);
            this.vertexColor[i] = quad.spriteColor(i, 0);
            this.vertexUV[i][0] = quad.spriteU(i, 0);
            this.vertexUV[i][1] = quad.spriteV(i, 0);
            this.vertexLight[i] = quad.lightmap(i);
            this.vertexNormal[i][0] = quad.normalX(i);
            this.vertexNormal[i][1] = quad.normalY(i);
            this.vertexNormal[i][2] = quad.normalZ(i);
        }
    }

    public void emit(QuadEmitter emitter){
        // Header
        emitter.cullFace(this.cullFace);
        emitter.nominalFace(this.nominalFace);
        emitter.colorIndex(this.colorIndex);
        emitter.material(this.material);
        emitter.tag(this.tag);
        // Vertices
        for(int i = 0; i < 4; i++){
            emitter.pos(i, this.vertexPos[i][0], this.vertexPos[i][1], this.vertexPos[i][2]);
            emitter.spriteColor(i, 0, this.vertexColor[i]);
            emitter.sprite(i, 0, this.vertexUV[i][0], this.vertexUV[i][1]);
            emitter.lightmap(i, this.vertexLight[i]);
            emitter.normal(i, this.vertexNormal[i][0], this.vertexNormal[i][1], this.vertexNormal[i][2]);
        }
        // Emit the quad
        emitter.emit();
    }
}
