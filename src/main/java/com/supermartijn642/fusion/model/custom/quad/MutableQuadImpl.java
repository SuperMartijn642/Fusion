package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import com.supermartijn642.fusion.util.PackedLightingHelper;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;

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
    private final float[][] uvs = new float[4][2];
    private Direction facing;
    private TextureAtlasSprite sprite;
    private int tintIndex = -1;
    private boolean shade = true;
    private int lightEmission = 0;
    // Our properties
    private boolean emissive = false;
    private BlockRenderLayer renderLayer;

    public MutableQuadImpl(){
        for(int i = 0; i < 4; i++)
            this.positions[i] = new Vector3f();
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        MutableQuadImpl impl = (MutableQuadImpl)quad;
        // Quad data
        this.bakedQuadCache = impl.bakedQuadCache;
        for(int i = 0; i < 4; i++){
            Vector3f position = impl.positions[i];
            this.positions[i].set(position.x(), position.y(), position.z());
            this.uvs[i][0] = impl.uvs[i][0];
            this.uvs[i][1] = impl.uvs[i][1];
        }
        this.facing = impl.facing;
        this.sprite = impl.sprite;
        this.tintIndex = impl.tintIndex;
        this.shade = impl.shade;
        this.lightEmission = impl.lightEmission;
        // Our properties
        this.emissive = impl.emissive;
        this.renderLayer = impl.renderLayer;
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        // Quad data
        this.bakedQuadCache = quad;
        VertexFormat format = quad.getFormat();
        for(int i = 0; i < 4; i++){
            BakedQuadHelper.getPosition(format, quad.getVertices(), i, this.positions[i]);
            BakedQuadHelper.getUV(format, quad.getVertices(), i, this.uvs[i]);
        }
        this.facing = quad.getDirection();
        this.sprite = quad.getSprite();
        this.tintIndex = quad.getTintIndex();
        this.shade = quad.shouldApplyDiffuseLighting();
        this.lightEmission = 15;
        if(BakedQuadHelper.hasLighting(format)){
            for(int i = 0; i < 4; i++){
                int lighting = BakedQuadHelper.getLighting(format, quad.getVertices(), i);
                lighting = Math.min(PackedLightingHelper.unpackBlock(lighting), PackedLightingHelper.unpackSky(lighting));
                if(lighting < this.lightEmission)
                    this.lightEmission = lighting;
            }
        }
        // Our properties
        this.emissive = false;
        this.renderLayer = null;
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
    public MutableQuad position(int vertexIndex, Vector3f position){
        this.positions[vertexIndex].set(position.x(), position.y(), position.z());
        this.facing = null;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public Vector3f position(int vertexIndex){
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
        this.uvs[vertexIndex][0] = u;
        this.uvs[vertexIndex][1] = v;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public float u(int vertexIndex){
        return this.uvs[vertexIndex][0];
    }

    @Override
    public float v(int vertexIndex){
        return this.uvs[vertexIndex][1];
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
    public MutableQuad renderLayer(BlockRenderLayer renderLayer){
        this.renderLayer = renderLayer;
        return this;
    }

    @Override
    public BlockRenderLayer renderLayer(){
        return this.renderLayer;
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
            VertexFormat format = BakedQuadHelper.getCombinedVertexFormat();
            int[] vertices = BakedQuadHelper.createVertices(format);
            for(int i = 0; i < 4; i++){
                BakedQuadHelper.setPosition(format, vertices, i, this.positions[i]);
                BakedQuadHelper.setColor(format, vertices, i, 0xFFFFFFFF);
                BakedQuadHelper.setUV(format, vertices, i, this.uvs[i]);
            }
            if(this.emissive || this.lightEmission > 0){
                int lightEmission = this.emissive ? 15 : this.lightEmission;
                int lighting = PackedLightingHelper.pack(lightEmission, lightEmission);
                for(int i = 0; i < 4; i++)
                    BakedQuadHelper.setLighting(format, vertices, i, lighting);
            }
            BakedQuadHelper.fillNormals(format, vertices);
            this.bakedQuadCache = new BakedQuad(
                vertices,
                this.tintIndex,
                this.facing,
                this.sprite,
                this.emissive || this.shade,
                format
            );
        }
        return this.bakedQuadCache;
    }
}
