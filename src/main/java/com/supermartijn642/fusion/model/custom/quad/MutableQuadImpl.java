package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import com.supermartijn642.fusion.util.ChunkRenderTypeHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector2f;
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
    private final Vector2f[] uvs = new Vector2f[4];
    private Direction facing;
    private TextureAtlasSprite sprite;
    private int tintIndex = -1;
    private boolean shade = true;
    private int lightEmission = 0;
    // Our properties
    private boolean ambientOcclusion = true;
    private boolean emissive = false;
    private RenderType chunkRenderType;
    private RenderType itemRenderType;

    public MutableQuadImpl(){
        for(int i = 0; i < 4; i++){
            this.positions[i] = new Vector3f();
            this.uvs[i] = new Vector2f();
        }
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        MutableQuadImpl impl = (MutableQuadImpl)quad;
        // Quad data
        this.bakedQuadCache = impl.bakedQuadCache;
        for(int i = 0; i < 4; i++){
            this.positions[i].set(impl.positions[i]);
            this.uvs[i].set(impl.uvs[i]);
        }
        this.facing = impl.facing;
        this.sprite = impl.sprite;
        this.tintIndex = impl.tintIndex;
        this.shade = impl.shade;
        this.lightEmission = impl.lightEmission;
        // Our properties
        this.ambientOcclusion = impl.ambientOcclusion;
        this.emissive = impl.emissive;
        this.chunkRenderType = impl.chunkRenderType;
        this.itemRenderType = impl.itemRenderType;
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        // Quad data
        this.bakedQuadCache = quad;
        for(int i = 0; i < 4; i++){
            BakedQuadHelper.getPosition(quad.getVertices(), i, this.positions[i]);
            BakedQuadHelper.getUV(quad.getVertices(), i, this.uvs[i]);
        }
        this.facing = quad.getDirection();
        this.sprite = quad.getSprite();
        this.tintIndex = quad.getTintIndex();
        this.shade = quad.isShade();
        this.lightEmission = quad.getLightEmission();
        // Our properties
        this.ambientOcclusion = quad.hasAmbientOcclusion();
        this.emissive = false;
        this.chunkRenderType = null;
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
        this.uvs[vertexIndex].set(u, v);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public float u(int vertexIndex){
        return this.uvs[vertexIndex].x();
    }

    @Override
    public float v(int vertexIndex){
        return this.uvs[vertexIndex].y();
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
    public MutableQuad renderTypes(RenderType chunkRenderType, RenderType itemRenderType){
        this.chunkRenderType(chunkRenderType);
        this.itemRenderType = itemRenderType;
        return this;
    }

    @Override
    public MutableQuad chunkRenderType(RenderType chunkRenderType){
        if(!ChunkRenderTypeHelper.isChunkRenderType(chunkRenderType))
            throw new IllegalArgumentException("Render type '" + chunkRenderType + "' is not a chunk render type!");
        this.chunkRenderType = chunkRenderType;
        return this;
    }

    @Override
    public RenderType chunkRenderType(){
        return this.chunkRenderType;
    }

    @Override
    public MutableQuad itemRenderType(RenderType itemRenderType){
        this.itemRenderType = itemRenderType;
        return this;
    }

    @Override
    public RenderType itemRenderType(){
        if(this.itemRenderType == null && this.chunkRenderType != null)
            return this.chunkRenderType == RenderType.translucent() ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet();
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
                this.facing = BakedQuadHelper.calculateFacing(this.positions[0], this.positions[1], this.positions[2]);
                if(this.facing == null)
                    this.facing = Direction.UP;
            }
            if(this.sprite == null)
                throw new IllegalStateException("No sprite was specified!");
            int[] vertices = BakedQuadHelper.createVertices();
            for(int i = 0; i < 4; i++){
                BakedQuadHelper.setPosition(vertices, i, this.positions[i]);
                BakedQuadHelper.setColor(vertices, i, -1);
                BakedQuadHelper.setUV(vertices, i, this.uvs[i]);
            }
            this.bakedQuadCache = new BakedQuad(
                vertices,
                this.tintIndex,
                this.facing,
                this.sprite,
                this.emissive || this.shade,
                this.emissive ? 15 : this.lightEmission,
                !this.emissive && this.ambientOcclusion
            );
        }
        return this.bakedQuadCache;
    }
}
