package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;

import java.util.Arrays;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private static final int VERTEX_SIZE;
    private static final int LIGHTMAP_OFFSET;
    private static final int UV_OFFSET;

    static{
        VertexFormat format = DefaultVertexFormats.BLOCK;
        VERTEX_SIZE = format.getVertexSize() / 4;
        LIGHTMAP_OFFSET = format.getOffset(format.getElements().indexOf(DefaultVertexFormats.ELEMENT_UV2)) / 4;
        UV_OFFSET = format.getOffset(format.getElements().indexOf(DefaultVertexFormats.ELEMENT_UV0)) / 4;
    }

    private static final int FULL_BRIGHT_LIGHTMAP = LightTexture.pack(15, 15);

    private final int[] vertices = new int[VERTEX_SIZE * 4];
    private int tintIndex;
    private Direction lightFace;
    private TextureAtlasSprite sprite;
    private boolean shade;
    private boolean emissive = false;

    public void fillFromBakedQuad(BakedQuad quad){
        System.arraycopy(quad.getVertices(), 0, this.vertices, 0, this.vertices.length);
        this.tintIndex = quad.getTintIndex();
        this.lightFace = quad.getDirection();
        this.sprite = quad.getSprite();
        this.shade = quad.shouldApplyDiffuseLighting();
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
                this.lightmap(i, FULL_BRIGHT_LIGHTMAP);
        }
        return new BakedQuad(Arrays.copyOf(this.vertices, this.vertices.length), this.tintIndex, this.lightFace, this.sprite, this.shade);
    }
}
