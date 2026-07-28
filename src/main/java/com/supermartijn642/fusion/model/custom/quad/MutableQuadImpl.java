package com.supermartijn642.fusion.model.custom.quad;

import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import com.supermartijn642.fusion.util.ChunkRenderTypeHelper;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

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
    private RenderType chunkRenderType;
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
        this.chunkRenderType = impl.chunkRenderType;
        this.itemRenderType = impl.itemRenderType;
        this.bakedQuadCache = impl.bakedQuadCache;
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        this.bakedQuadCache = quad;
        for(int i = 0; i < 4; i++){
            int offset = i * VERTEX_SIZE + VERTEX_POSITION;
            this.vertices[offset] = BakedQuadHelper.getPosX(quad.getVertices(), i);
            this.vertices[offset + 1] = BakedQuadHelper.getPosY(quad.getVertices(), i);
            this.vertices[offset + 2] = BakedQuadHelper.getPosZ(quad.getVertices(), i);
            offset = i * VERTEX_SIZE + VERTEX_UV;
            this.vertices[offset] = BakedQuadHelper.getU(quad.getVertices(), i);
            this.vertices[offset + 1] = BakedQuadHelper.getV(quad.getVertices(), i);
        }
        this.facing = quad.getDirection();
        this.sprite = quad.getSprite();
        this.tintIndex = quad.getTintIndex();
        this.flags = 0;
        if(quad.isShade())
            this.flags |= (1 << SHADE);
        int lightEmission = 15;
        for(int i = 0; i < 4; i++){
            int lighting = BakedQuadHelper.getLighting(quad.getVertices(), i);
            lighting = Math.min(LightTexture.block(lighting), LightTexture.sky(lighting));
            if(lighting < lightEmission)
                lightEmission = lighting;
        }
        this.flags |= (lightEmission << LIGHT_EMISSION);
        this.flags |= (1 << AMBIENT_OCCLUSION);
        this.chunkRenderType = null;
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
            this.vertices[offset] = quad.spriteU(i, 0);
            this.vertices[offset + 1] = quad.spriteV(i, 0);
        }
        this.facing = null;
        this.sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)).find(quad, 0);
        this.tintIndex = quad.colorIndex();
        this.flags = 0;
        RenderMaterialImpl material = quad.material() instanceof RenderMaterialImpl impl ? impl : null;
        if(material == null || !material.disableDiffuse(0))
            this.flags |= (1 << SHADE);
        int lightEmission = 15;
        for(int i = 0; i < 4; i++){
            int lighting = quad.lightmap(i);
            lighting = Math.min(LightTexture.block(lighting), LightTexture.sky(lighting));
            if(lighting < lightEmission)
                lightEmission = lighting;
        }
        this.flags |= (lightEmission << LIGHT_EMISSION);
        if(material == null || !material.disableAo(0))
            this.flags |= (1 << AMBIENT_OCCLUSION);
        if(material != null && material.emissive(0))
            this.flags |= (1 << EMISSIVE);
        this.chunkRenderType = material == null ? null : material.blendMode(0).blockRenderLayer;
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
    public MutableQuad position(int vertexIndex, Vector3f position){
        return this.position(vertexIndex, position.x(), position.y(), position.z());
    }

    @Override
    public Vector3f copyPosition(int vertexIndex, @Nullable Vector3f dest){
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
    public Vector3f position(int vertexIndex){
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
    public RenderType itemRenderType(boolean fabulous){
        if(this.itemRenderType == null && this.chunkRenderType != null)
            return this.chunkRenderType == RenderType.translucent() ?
                fabulous ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet() :
                Sheets.cutoutBlockSheet();
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
        lightEmission = Math.min(Math.max(lightEmission, 0), 15);
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
            if(this.sprite == null)
                throw new IllegalStateException("No sprite was specified!");
            int[] vertices = BakedQuadHelper.createVertices();
            for(int i = 0; i < 4; i++){
                int offset = i * VERTEX_SIZE + VERTEX_POSITION;
                BakedQuadHelper.setPosition(vertices, i, this.vertices[offset], this.vertices[offset + 1], this.vertices[offset + 2]);
                BakedQuadHelper.setColor(vertices, i, -1);
                offset = i * VERTEX_SIZE + VERTEX_UV;
                BakedQuadHelper.setUV(vertices, i, this.vertices[offset], this.vertices[offset + 1]);
            }
            boolean emissive = this.emissive();
            int lightEmission = emissive ? 15 : this.lightEmission();
            if(lightEmission > 0){
                int lighting = LightTexture.pack(lightEmission, lightEmission);
                for(int i = 0; i < 4; i++)
                    BakedQuadHelper.setLighting(vertices, i, lighting);
            }
            this.bakedQuadCache = new BakedQuad(
                vertices,
                this.tintIndex,
                this.facing,
                this.sprite,
                !emissive && this.shade()
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
            quad.sprite(i, 0, this.vertices[offset], this.vertices[offset + 1]);
            quad.spriteColor(i, 0, 0xFFFFFFFF);
        }

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

        quad.colorIndex(this.tintIndex);
        boolean emissive = this.emissive();
        quad.material(
            RendererAccess.INSTANCE.getRenderer().materialFinder()
                .blendMode(0, BlendMode.fromRenderLayer(this.chunkRenderType))
                .disableDiffuse(0, emissive || !this.shade())
                .disableAo(0, emissive || !this.ambientOcclusion())
                .emissive(0, emissive)
                .find()
        );
    }
}
