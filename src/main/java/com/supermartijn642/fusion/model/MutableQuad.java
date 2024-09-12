package com.supermartijn642.fusion.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.Arrays;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private static final int VERTEX_SIZE;
    private static final int LIGHTMAP_OFFSET;
    private static final int UV_OFFSET;

    static{
        VertexFormat format = DefaultVertexFormat.BLOCK;
        VERTEX_SIZE = format.getVertexSize() / 4;
        LIGHTMAP_OFFSET = format.getOffset(VertexFormatElement.UV2) / 4;
        UV_OFFSET = format.getOffset(VertexFormatElement.UV) / 4;
    }

    private final int[] vertices = new int[VERTEX_SIZE * 4];
    private int tintIndex;
    private Direction lightFace;
    private TextureAtlasSprite sprite;
    private boolean shade;
    private boolean hasAmbientOcclusion;
    private boolean emissive = false;

    public void fillFromBakedQuad(BakedQuad quad){
        System.arraycopy(quad.getVertices(), 0, this.vertices, 0, this.vertices.length);
        this.tintIndex = quad.getTintIndex();
        this.lightFace = quad.getDirection();
        this.sprite = quad.getSprite();
        this.shade = quad.isShade();
        this.hasAmbientOcclusion = quad.hasAmbientOcclusion();
        this.emissive = false;
    }

    public void emissive(boolean emissive){
        this.emissive = emissive;
    }

    public void lightmap(int vertexIndex, int lightmap){
        int offset = vertexIndex * VERTEX_SIZE + LIGHTMAP_OFFSET;
        this.vertices[offset] = lightmap;
    }

    public int lightmap(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + LIGHTMAP_OFFSET;
        return this.vertices[offset];
    }

    public void ambientOcclusion(boolean ambientOcclusion){
        this.hasAmbientOcclusion = ambientOcclusion;
    }

    public void uv(int vertexIndex, float u, float v){
        int offset = vertexIndex * VERTEX_SIZE + UV_OFFSET;
        this.vertices[offset] = Float.floatToRawIntBits(u);
        this.vertices[offset + 1] = Float.floatToRawIntBits(v);
    }

    public float u(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + UV_OFFSET;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public float v(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + UV_OFFSET + 1;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public BakedQuad toBakedQuad(){
        if(this.emissive){
            for(int i = 0; i < 4; i++)
                this.lightmap(i, LightTexture.FULL_BRIGHT);
        }
        return new BakedQuad(Arrays.copyOf(this.vertices, this.vertices.length), this.tintIndex, this.lightFace, this.sprite, this.shade, this.hasAmbientOcclusion);
    }
}
