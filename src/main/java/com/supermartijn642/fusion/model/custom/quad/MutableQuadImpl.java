package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuadImpl implements MutableQuad {

    public static MutableQuad create(){
        return new MutableQuadImpl();
    }

    // Quad data
    private BakedQuad bakedQuadCache;
    private final Vector3f[] positions = new Vector3f[4];
    private final long[] uvs = new long[4];
    private Direction facing;
    private TextureAtlasSprite sprite;
    private int tintIndex = -1;
    private boolean shade = true;
    private int lightEmission = 0;
    // Our properties
    private boolean ambientOcclusion = true;
    private boolean emissive = false;
    private ChunkSectionLayer chunkLayer;
    private RenderType itemRenderType;

    public MutableQuadImpl(){
        for(int i = 0; i < 4; i++)
            this.positions[i] = new Vector3f();
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        MutableQuadImpl impl = (MutableQuadImpl)quad;
        // Quad data
        this.bakedQuadCache = impl.bakedQuadCache;
        for(int i = 0; i < 4; i++)
            this.positions[i].set(impl.positions[i]);
        System.arraycopy(impl.uvs, 0, this.uvs, 0, 4);
        this.facing = impl.facing;
        this.sprite = impl.sprite;
        this.tintIndex = impl.tintIndex;
        this.shade = impl.shade;
        this.lightEmission = impl.lightEmission;
        // Our properties
        this.ambientOcclusion = impl.ambientOcclusion;
        this.emissive = impl.emissive;
        this.chunkLayer = impl.chunkLayer;
        this.itemRenderType = impl.itemRenderType;
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        // Quad data
        this.bakedQuadCache = quad;
        for(int i = 0; i < 4; i++){
            this.positions[i].set(quad.position(i));
            this.uvs[i] = quad.packedUV(i);
        }
        this.facing = quad.direction();
        this.sprite = quad.sprite();
        this.tintIndex = quad.tintIndex();
        this.shade = quad.shade();
        this.lightEmission = quad.lightEmission();
        // Our properties
        this.ambientOcclusion = quad.ambientOcclusion();
        this.emissive = false;
        this.chunkLayer = null;
        this.itemRenderType = null;
        return this;
    }

    @Override
    public MutableQuad createCopy(){
        return new MutableQuadImpl().copyFrom(this);
    }

    @Override
    public MutableQuad position(int vertexIndex, float x, float y, float z){
        this.positions[vertexIndex].set(x, y, z);
        this.facing = null;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad position(int vertexIndex, Vector3fc position){
        this.positions[vertexIndex].set(position);
        this.facing = null;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public Vector3fc position(int vertexIndex){
        return this.positions[vertexIndex];
    }

    @Override
    public float x(int vertexIndex){
        return this.positions[vertexIndex].x();
    }

    @Override
    public float y(int vertexIndex){
        return this.positions[vertexIndex].y();
    }

    @Override
    public float z(int vertexIndex){
        return this.positions[vertexIndex].z();
    }

    @Override
    public MutableQuad uv(int vertexIndex, float u, float v){
        this.uvs[vertexIndex] = UVPair.pack(u, v);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public float u(int vertexIndex){
        return UVPair.unpackU(this.uvs[vertexIndex]);
    }

    @Override
    public float v(int vertexIndex){
        return UVPair.unpackV(this.uvs[vertexIndex]);
    }

    @Override
    public Direction facing(){
        return this.facing;
    }

    @Override
    public MutableQuad sprite(TextureAtlasSprite sprite){
        this.sprite = sprite;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public TextureAtlasSprite sprite(){
        return this.sprite;
    }

    @Override
    public MutableQuad renderLayers(ChunkSectionLayer chunkLayer, RenderType itemRenderType){
        this.chunkLayer = chunkLayer;
        this.itemRenderType = itemRenderType;
        return this;
    }

    @Override
    public MutableQuad chunkLayer(ChunkSectionLayer chunkLayer){
        this.chunkLayer = chunkLayer;
        return this;
    }

    @Override
    public ChunkSectionLayer chunkLayer(){
        return this.chunkLayer;
    }

    @Override
    public MutableQuad itemRenderType(RenderType itemRenderType){
        this.itemRenderType = itemRenderType;
        return this;
    }

    @Override
    public RenderType itemRenderType(){
        if(this.itemRenderType == null && this.chunkLayer != null){
            return TextureAtlas.LOCATION_BLOCKS.equals(this.sprite.atlasLocation()) ?
                this.chunkLayer == ChunkSectionLayer.TRANSLUCENT ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockSheet() :
                Sheets.translucentItemSheet();
        }
        return this.itemRenderType;
    }

    @Override
    public MutableQuad tintIndex(int tintIndex){
        this.tintIndex = tintIndex;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public int tintIndex(){
        return this.tintIndex;
    }

    @Override
    public MutableQuad shade(boolean shade){
        this.shade = shade;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean shade(){
        return this.shade;
    }

    @Override
    public MutableQuad lightEmission(int lightEmission){
        this.lightEmission = lightEmission;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public int lightEmission(){
        return this.lightEmission;
    }

    @Override
    public MutableQuad ambientOcclusion(boolean ambientOcclusion){
        this.ambientOcclusion = ambientOcclusion;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean ambientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public MutableQuad emissive(boolean emissive){
        this.emissive = emissive;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean emissive(){
        return this.emissive;
    }

    private void invalidateBakedQuadCache(){
        this.bakedQuadCache = null;
    }

    public BakedQuad toBakedQuad(){
        if(this.bakedQuadCache == null){
            if(this.facing == null){
                this.facing = FaceBakery.calculateFacing(this.positions);
                if(this.facing == null)
                    this.facing = Direction.UP;
            }
            if(this.sprite == null)
                throw new IllegalStateException("No sprite was specified!");
            this.bakedQuadCache = new BakedQuad(
                new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
                this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
                this.tintIndex,
                this.facing,
                this.sprite,
                !this.emissive && this.shade,
                this.emissive ? 15 : this.lightEmission,
                !this.emissive && this.ambientOcclusion
            );
        }
        return this.bakedQuadCache;
    }
}
