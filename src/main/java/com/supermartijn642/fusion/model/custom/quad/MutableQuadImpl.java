package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import com.supermartijn642.fusion.util.ChunkRenderTypeHelper;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
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

    static {
        if(ChunkRenderTypeHelper.all().size() > 31)
            throw new AssertionError("More than 31 chunk render types!");
    }

    // Flags
    private static final int SHADE = 0;
    private static final int LIGHT_EMISSION = 1; // 4 bits
    private static final int AMBIENT_OCCLUSION = 5;
    private static final int EMISSIVE = 6;
    private static final int FACING = 7; // 3 bits
    private static final int CHUNK_RENDER_TYPE = 10; // 5 bits
    // Vertices
    private static final int VERTEX_SIZE = 3 + 2;
    private static final int VERTEX_POSITION = 0; // 3 floats
    private static final int VERTEX_UV = 3; // 2 floats

    private final float[] vertices = new float[4 * VERTEX_SIZE];
    private int flags;
    private int tintIndex = -1;
    private TextureAtlasSprite sprite;

    private BakedQuad bakedQuadCache;

    public MutableQuadImpl(){
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        MutableQuadImpl impl = (MutableQuadImpl)quad;
        System.arraycopy(impl.vertices, 0, this.vertices, 0, this.vertices.length);
        this.flags = impl.flags;
        this.sprite = impl.sprite;
        this.tintIndex = impl.tintIndex;
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
        this.sprite = quad.getSprite();
        this.tintIndex = quad.getTintIndex();
        this.flags = 0;
        this.flags |= ((quad.getDirection().ordinal() + 1) << FACING);
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
        this.sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)).find(quad);
        this.tintIndex = quad.colorIndex();
        this.flags = 0;
        if(!quad.material().disableDiffuse())
            this.flags |= (1 << SHADE);
        int lightEmission = 15;
        for(int i = 0; i < 4; i++){
            int lighting = quad.lightmap(i);
            lighting = Math.min(LightTexture.block(lighting), LightTexture.sky(lighting));
            if(lighting < lightEmission)
                lightEmission = lighting;
        }
        this.flags |= (lightEmission << LIGHT_EMISSION);
        if(quad.material().ambientOcclusion() != TriState.FALSE)
            this.flags |= (1 << AMBIENT_OCCLUSION);
        if(quad.material().emissive())
            this.flags |= (1 << EMISSIVE);
        this.flags |= (ChunkRenderTypeHelper.getId(quad.material().blendMode().blockRenderLayer) << CHUNK_RENDER_TYPE);
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
        this.flags &= ~(7 << FACING);
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
        int ordinal = (this.flags >> FACING) & 7;
        if(ordinal == 0){
            int offsetV0 = VERTEX_POSITION;
            int offsetV1 = VERTEX_SIZE + VERTEX_POSITION;
            int offsetV2 = 2 * VERTEX_SIZE + VERTEX_POSITION;
            Direction facing = BakedQuadHelper.calculateFacing(
                this.vertices[offsetV0], this.vertices[offsetV0 + 1], this.vertices[offsetV0 + 2],
                this.vertices[offsetV1], this.vertices[offsetV1 + 1], this.vertices[offsetV1 + 2],
                this.vertices[offsetV2], this.vertices[offsetV2 + 1], this.vertices[offsetV2 + 2]
            );
            if(facing == null)
                facing = Direction.UP;
            this.flags |= ((facing.ordinal() + 1) << FACING);
            return facing;
        }
        return Direction.values()[ordinal - 1];
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
    public MutableQuad chunkRenderType(RenderType chunkRenderType){
        if(!ChunkRenderTypeHelper.isChunkRenderType(chunkRenderType))
            throw new IllegalArgumentException("Render type '" + chunkRenderType + "' is not a chunk render type!");
        this.flags = (this.flags & ~(31 << CHUNK_RENDER_TYPE)) | (ChunkRenderTypeHelper.getId(chunkRenderType) << CHUNK_RENDER_TYPE);
        return this;
    }

    @Override
    public RenderType chunkRenderType(){
        return ChunkRenderTypeHelper.byId((this.flags >> CHUNK_RENDER_TYPE) & 31);
    }

    @Override
    public RenderType itemRenderType(){
        RenderType chunkRenderType = this.chunkRenderType();
        if(chunkRenderType == null)
            return null;
        return chunkRenderType == RenderType.translucent() ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet();
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
                this.facing(),
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
            quad.uv(i, this.vertices[offset], this.vertices[offset + 1]);
        }
        quad.color(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);

        int lightEmission = this.lightEmission();
        int lightmap = LightTexture.pack(lightEmission, lightEmission);
        quad.lightmap(lightmap, lightmap, lightmap, lightmap);

        Direction facing = this.facing();
        for(int i = 0; i < 4; i++)
            quad.normal(0, facing.getStepX(), facing.getStepY(), facing.getStepZ());
        quad.nominalFace(facing);

        quad.colorIndex(this.tintIndex);
        boolean emissive = this.emissive();
        quad.material(
            RendererAccess.INSTANCE.getRenderer().materialFinder()
                .blendMode(BlendMode.fromRenderLayer(this.chunkRenderType()))
                .disableDiffuse(emissive || !this.shade())
                .ambientOcclusion(!emissive && this.ambientOcclusion() ? TriState.TRUE : TriState.FALSE)
                .emissive(emissive)
                .shadeMode(ShadeMode.VANILLA)
                .find()
        );
    }
}
