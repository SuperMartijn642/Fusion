package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuadImpl implements MutableQuad {

    public static MutableQuad create(){
        return new MutableQuadImpl();
    }

    // Flags
    private static final int SHADE = 0;
    private static final int LIGHT_EMISSION = 1;
    private static final int AMBIENT_OCCLUSION = 5;
    private static final int EMISSIVE = 6;
    // Vertices
    private static final int VERTEX_SIZE = 3 + 2;
    private static final int VERTEX_POSITION = 0;
    private static final int VERTEX_UV = 3;

    private final float[] vertices = new float[4 * VERTEX_SIZE];
    private int flags;
    private int tintIndex = -1;
    private Direction facing;
    private TextureAtlasSprite sprite;
    private ChunkSectionLayer chunkLayer;
    private RenderType itemRenderType;

    private BakedQuad bakedQuadCache;

    public MutableQuadImpl(){
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        MutableQuadImpl impl = (MutableQuadImpl)quad;
        System.arraycopy(impl.vertices, 0, this.vertices, 0, this.vertices.length);
        this.flags = impl.flags;
        this.facing = impl.facing;
        this.sprite = impl.sprite;
        this.tintIndex = impl.tintIndex;
        this.chunkLayer = impl.chunkLayer;
        this.itemRenderType = impl.itemRenderType;
        this.bakedQuadCache = impl.bakedQuadCache;
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        this.bakedQuadCache = quad;
        for(int i = 0; i < 4; i++){
            int offset = i * VERTEX_SIZE + VERTEX_POSITION;
            Vector3fc position = quad.position(i);
            this.vertices[offset] = position.x();
            this.vertices[offset + 1] = position.y();
            this.vertices[offset + 2] = position.z();
            offset = i * VERTEX_SIZE + VERTEX_UV;
            long packedUV = quad.packedUV(i);
            this.vertices[offset] = UVPair.unpackU(packedUV);
            this.vertices[offset + 1] = UVPair.unpackV(packedUV);
        }
        this.facing = quad.direction();
        this.sprite = quad.sprite();
        this.tintIndex = quad.tintIndex();
        this.flags = 0;
        if(quad.shade())
            this.flags |= (1 << SHADE);
        this.flags |= (quad.lightEmission() << LIGHT_EMISSION);
        this.flags |= (1 << AMBIENT_OCCLUSION);
        this.chunkLayer = null;
        this.itemRenderType = null;
        return this;
    }

    @Override
    public MutableQuad copyFrapiQuad(QuadView quad){
        // Quad data
        this.bakedQuadCache = null;
        for(int i = 0; i < 4; i++){
            int offset = i * VERTEX_SIZE + VERTEX_POSITION;
            this.vertices[offset] = quad.x(i);
            this.vertices[offset + 1] = quad.y(i);
            this.vertices[offset + 2] = quad.z(i);
            offset = i * VERTEX_SIZE + VERTEX_UV;
            this.vertices[offset] = quad.u(i);
            this.vertices[offset + 1] = quad.v(i);
        }
        this.facing = null;
        Identifier atlasId = quad.atlas() == QuadAtlas.BLOCK ? AtlasIds.BLOCKS : AtlasIds.ITEMS;
        this.sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(atlasId).spriteFinder().find(quad);
        this.tintIndex = quad.tintIndex();
        this.flags = 0;
        if(quad.diffuseShade())
            this.flags |= (1 << SHADE);
        int lightEmission = 15;
        for(int i = 0; i < 4; i++){
            int lightmap = quad.lightmap(i);
            if(lightmap == 0){
                lightEmission = 0;
                break;
            }
            int blockLight = LightTexture.block(lightmap);
            int skyLight = LightTexture.sky(lightmap);
            lightEmission = Math.min(lightEmission, Math.min(blockLight, skyLight));
        }
        this.flags |= (lightEmission << LIGHT_EMISSION);
        if(quad.ambientOcclusion() != TriState.FALSE)
            this.flags |= (1 << AMBIENT_OCCLUSION);
        if(quad.emissive())
            this.flags |= (1 << EMISSIVE);
        this.chunkLayer = quad.renderLayer();
        this.itemRenderType = null;
        return this;
    }

    @Override
    public MutableQuad createCopy(){
        return new MutableQuadImpl().copyFrom(this);
    }

    @Override
    public MutableQuad position(int vertexIndex, float x, float y, float z){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION;
        this.vertices[offset] = x;
        this.vertices[offset + 1] = y;
        this.vertices[offset + 2] = z;
        this.facing = null;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad position(int vertexIndex, Vector3fc position){
        return this.position(vertexIndex, position.x(), position.y(), position.z());
    }

    @Override
    public Vector3fc copyPosition(int vertexIndex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION;
        dest.set(
            this.vertices[offset],
            this.vertices[offset + 1],
            this.vertices[offset + 2]
        );
        return dest;
    }

    @Override
    public Vector3fc position(int vertexIndex){
        return this.copyPosition(vertexIndex, null);
    }

    @Override
    public float x(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION;
        return this.vertices[offset];
    }

    @Override
    public float y(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION;
        return this.vertices[offset + 1];
    }

    @Override
    public float z(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_POSITION;
        return this.vertices[offset + 2];
    }

    @Override
    public MutableQuad uv(int vertexIndex, float u, float v){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_UV;
        this.vertices[offset] = u;
        this.vertices[offset + 1] = v;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public float u(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_UV;
        return this.vertices[offset];
    }

    @Override
    public float v(int vertexIndex){
        int offset = vertexIndex * VERTEX_SIZE + VERTEX_UV;
        return this.vertices[offset + 1];
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
        this.flags = shade ? this.flags | (1 << SHADE) : this.flags & ~(1 << SHADE);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean shade(){
        return (this.flags & (1 << SHADE)) != 0;
    }

    @Override
    public MutableQuad lightEmission(int lightEmission){
        lightEmission = Math.clamp(lightEmission, 0, 15);
        this.flags &= ~(15 << LIGHT_EMISSION) | (lightEmission << LIGHT_EMISSION);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public int lightEmission(){
        return (this.flags >> LIGHT_EMISSION) & 15;
    }

    @Override
    public MutableQuad ambientOcclusion(boolean ambientOcclusion){
        this.flags = ambientOcclusion ? this.flags | (1 << AMBIENT_OCCLUSION) : this.flags & ~(1 << AMBIENT_OCCLUSION);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean ambientOcclusion(){
        return (this.flags & (1 << AMBIENT_OCCLUSION)) != 0;
    }

    @Override
    public MutableQuad emissive(boolean emissive){
        this.flags = emissive ? this.flags | (1 << EMISSIVE) : this.flags & ~(1 << EMISSIVE);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public boolean emissive(){
        return (this.flags & (1 << EMISSIVE)) != 0;
    }

    private void invalidateBakedQuadCache(){
        this.bakedQuadCache = null;
    }

    public BakedQuad toBakedQuad(){
        if(this.bakedQuadCache == null){
            if(this.facing == null){
                int offsetV0 = VERTEX_POSITION;
                int offsetV1 = VERTEX_SIZE + VERTEX_POSITION;
                int offsetV2 = 2 * VERTEX_SIZE + VERTEX_POSITION;
                this.facing = BakedQuadHelper.calculateFacing(
                    this.vertices[offsetV0], this.vertices[offsetV0 + 1], this.vertices[offsetV0 + 2],
                    this.vertices[offsetV1], this.vertices[offsetV1 + 1], this.vertices[offsetV1 + 2],
                    this.vertices[offsetV2], this.vertices[offsetV2 + 1], this.vertices[offsetV2 + 2]
                );
                if(this.facing == null)
                    this.facing = Direction.UP;
            }
            boolean emissive = this.emissive();
            this.bakedQuadCache = new BakedQuad(
                this.position(0), this.position(1), this.position(2), this.position(3),
                UVPair.pack(this.u(0), this.v(0)), UVPair.pack(this.u(1), this.v(1)), UVPair.pack(this.u(2), this.v(2)), UVPair.pack(this.u(3), this.v(3)),
                this.tintIndex,
                this.facing,
                this.sprite,
                !emissive && this.shade(),
                emissive ? 15 : this.lightEmission()
            );
        }
        return this.bakedQuadCache;
    }

    @Override
    public void toFrapiQuad(MutableQuadView quad){
        if(this.sprite == null)
            throw new IllegalStateException("No sprite was specified!");
        // Quad data
        for(int i = 0; i < 4; i++){
            int offset = i * VERTEX_SIZE + VERTEX_POSITION;
            quad.pos(i, this.vertices[offset], this.vertices[offset + 1], this.vertices[offset + 2]);
            offset = i * VERTEX_SIZE + VERTEX_UV;
            quad.uv(i, this.vertices[offset], this.vertices[offset + 1]);
        }
        quad.color(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);

        int lightEmission = this.lightEmission();
        int lightmap = LightTexture.pack(lightEmission, lightEmission);
        quad.lightmap(lightmap, lightmap, lightmap, lightmap);

        if(this.facing == null){
            int offsetV0 = VERTEX_POSITION;
            int offsetV1 = VERTEX_SIZE + VERTEX_POSITION;
            int offsetV2 = 2 * VERTEX_SIZE + VERTEX_POSITION;
            this.facing = BakedQuadHelper.calculateFacing(
                this.vertices[offsetV0], this.vertices[offsetV0 + 1], this.vertices[offsetV0 + 2],
                this.vertices[offsetV1], this.vertices[offsetV1 + 1], this.vertices[offsetV1 + 2],
                this.vertices[offsetV2], this.vertices[offsetV2 + 1], this.vertices[offsetV2 + 2]
            );
            if(this.facing == null)
                this.facing = Direction.UP;
        }
        for(int i = 0; i < 4; i++)
            quad.normal(0, this.facing.getStepX(), this.facing.getStepY(), this.facing.getStepZ());
        quad.nominalFace(this.facing);

        QuadAtlas atlas = QuadAtlas.of(this.sprite.atlasLocation());
        if(atlas == null)
            atlas = QuadAtlas.BLOCK;
        quad.atlas(atlas);

        quad.renderLayer(this.chunkLayer);
        quad.tintIndex(this.tintIndex);
        boolean emissive = this.emissive();
        quad.diffuseShade(!emissive && this.shade());
        quad.ambientOcclusion(!emissive && this.ambientOcclusion() ? TriState.TRUE : TriState.FALSE);
        quad.emissive(emissive);
    }
}
