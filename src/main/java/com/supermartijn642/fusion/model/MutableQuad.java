package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;

import java.util.Arrays;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private static final int FULL_BRIGHT_LIGHTMAP = 15 << 20 | 15 << 4;

    private int[] vertices = new int[DefaultVertexFormats.BLOCK.getIntegerSize() * 4];
    private VertexFormat vertexFormat;
    private int tintIndex;
    private EnumFacing lightFace;
    private TextureAtlasSprite sprite;
    private boolean shade;
    private boolean emissive = false;

    public void fillFromBakedQuad(BakedQuad quad){
        if(this.vertices.length < quad.getVertexData().length)
            this.vertices = new int[quad.getVertexData().length];
        System.arraycopy(quad.getVertexData(), 0, this.vertices, 0, quad.getVertexData().length);
        this.vertexFormat = quad.getFormat();
        this.tintIndex = quad.getTintIndex();
        this.lightFace = quad.getFace();
        this.sprite = quad.getSprite();
        this.shade = quad.shouldApplyDiffuseLighting();
        this.emissive = false;
    }

    public void emissive(boolean emissive){
        this.emissive = emissive;
    }

    public void lightmap(int vertexIndex, int lightmap){
        if(!this.vertexFormat.hasUvOffset(1))
            this.addVertexFormatElement(DefaultVertexFormats.TEX_2S);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(1) / 4;
        this.vertices[offset] = lightmap;
    }

    public int lightmap(int vertexIndex){
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(1) / 4;
        return this.vertices[offset];
    }

    public void uv(int vertexIndex, float u, float v){
        if(!this.vertexFormat.hasUvOffset(0))
            this.addVertexFormatElement(DefaultVertexFormats.TEX_2F);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(0) / 4;
        this.vertices[offset] = Float.floatToRawIntBits(u);
        this.vertices[offset + 1] = Float.floatToRawIntBits(v);
    }

    public float u(int vertexIndex){
        if(!this.vertexFormat.hasUvOffset(0))
            this.addVertexFormatElement(DefaultVertexFormats.TEX_2F);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(0) / 4;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public float v(int vertexIndex){
        if(!this.vertexFormat.hasUvOffset(0))
            this.addVertexFormatElement(DefaultVertexFormats.TEX_2F);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffsetById(0) / 4 + 1;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public BakedQuad toBakedQuad(){
        if(this.emissive){
            for(int i = 0; i < 4; i++)
                this.lightmap(i, FULL_BRIGHT_LIGHTMAP);
        }
        return new BakedQuad(Arrays.copyOf(this.vertices, this.vertexFormat.getIntegerSize() * 4), this.tintIndex, this.lightFace, this.sprite, this.shade, this.vertexFormat);
    }

    private void addVertexFormatElement(VertexFormatElement element){
        VertexFormat newFormat = new VertexFormat(this.vertexFormat).addElement(element);
        if(element.getSize() % 4 != 0){
            for(int i = 4 - (element.getSize() % 4); i > 0; i--)
                newFormat = newFormat.addElement(DefaultVertexFormats.PADDING_1B);
        }
        int[] arr = new int[Math.max(newFormat.getIntegerSize() * 4, this.vertices.length)];
        for(int i = 0; i < 4; i++)
            System.arraycopy(this.vertices, this.vertexFormat.getIntegerSize() * i, arr, newFormat.getIntegerSize() * i, this.vertexFormat.getIntegerSize());
        this.vertices = arr;
        this.vertexFormat = newFormat;
    }
}
