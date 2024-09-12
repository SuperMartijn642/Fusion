package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;

import java.util.Arrays;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuad {

    private static final int FULL_BRIGHT_LIGHTMAP = 15 << 20 | 15 << 4;

    private int[] vertices;
    private VertexFormat vertexFormat;
    private int tintIndex;
    private Direction lightFace;
    private TextureAtlasSprite sprite;
    private boolean shade;
    private boolean emissive = false;

    protected MutableQuad(int[] vertexData){
        this.vertices = vertexData;
    }

    public MutableQuad(){
        this(new int[DefaultVertexFormats.BLOCK_NORMALS.getIntegerSize() * 4]);
    }

    public void fillFromBakedQuad(BakedQuad quad){
        if(this.vertices.length < quad.getVertices().length)
            this.vertices = new int[quad.getVertices().length];
        System.arraycopy(quad.getVertices(), 0, this.vertices, 0, quad.getVertices().length);
        this.vertexFormat = quad.getFormat();
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
        if(!this.vertexFormat.hasUv(1))
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_UV1);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffset(1) / 4;
        this.vertices[offset] = lightmap;
    }

    public int lightmap(int vertexIndex){
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffset(1) / 4;
        return this.vertices[offset];
    }

    public void uv(int vertexIndex, float u, float v){
        if(!this.vertexFormat.hasUv(0))
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_UV1);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffset(0) / 4;
        this.vertices[offset] = Float.floatToRawIntBits(u);
        this.vertices[offset + 1] = Float.floatToRawIntBits(v);
    }

    public float u(int vertexIndex){
        if(!this.vertexFormat.hasUv(0))
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_UV0);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffset(0) / 4;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public float v(int vertexIndex){
        if(!this.vertexFormat.hasUv(0))
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_UV0);
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getUvOffset(0) / 4 + 1;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public void pos(int vertexIndex, float x, float y, float z){
        int elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        if(elementIndex < 0){
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_POSITION);
            elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        }
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getOffset(elementIndex) / 4;
        this.vertices[offset] = Float.floatToRawIntBits(x);
        this.vertices[offset + 1] = Float.floatToRawIntBits(y);
        this.vertices[offset + 2] = Float.floatToRawIntBits(z);
    }

    public float x(int vertexIndex){
        int elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        if(elementIndex < 0){
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_POSITION);
            elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        }
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getOffset(elementIndex) / 4;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public float y(int vertexIndex){
        int elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        if(elementIndex < 0){
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_POSITION);
            elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        }
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getOffset(elementIndex) / 4 + 1;
        return Float.intBitsToFloat(this.vertices[offset]);
    }

    public float z(int vertexIndex){
        int elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        if(elementIndex < 0){
            this.addVertexFormatElement(DefaultVertexFormats.ELEMENT_POSITION);
            elementIndex = this.vertexFormat.getElements().indexOf(DefaultVertexFormats.ELEMENT_POSITION);
        }
        int offset = vertexIndex * this.vertexFormat.getIntegerSize() + this.vertexFormat.getOffset(elementIndex) / 4 + 1;
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
        if(element.getByteSize() % 4 != 0){
            for(int i = 4 - (element.getByteSize() % 4); i > 0; i--)
                newFormat = newFormat.addElement(DefaultVertexFormats.ELEMENT_PADDING);
        }
        int[] arr = new int[Math.max(newFormat.getIntegerSize() * 4, this.vertices.length)];
        for(int i = 0; i < 4; i++)
            System.arraycopy(this.vertices, this.vertexFormat.getIntegerSize() * i, arr, newFormat.getIntegerSize() * i, this.vertexFormat.getIntegerSize());
        this.vertices = arr;
        this.vertexFormat = newFormat;
    }
}
