package com.supermartijn642.fusion.model.custom.quad;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
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

    // NeoForge data
    private BakedNormals bakedNormalsCache = BakedNormals.UNSPECIFIED;
    private final int[] bakedNormals = new int[4];
    private BakedColors bakedColorsCache = BakedColors.DEFAULT;
    private final int[] bakedColors = new int[4];

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
        // NeoForge data
        this.bakedNormalsCache = impl.bakedNormalsCache;
        System.arraycopy(impl.bakedNormals, 0, this.bakedNormals, 0, 4);
        this.bakedColorsCache = impl.bakedColorsCache;
        System.arraycopy(impl.bakedColors, 0, this.bakedColors, 0, 4);
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
        if(quad.hasAmbientOcclusion())
            this.flags |= (1 << AMBIENT_OCCLUSION);
        this.chunkLayer = null;
        this.itemRenderType = null;
        // NeoForge data
        this.bakedNormalsCache = quad.bakedNormals();
        this.bakedColorsCache = quad.bakedColors();
        for(int i = 0; i < 4; i++){
            this.bakedNormals[i] = this.bakedNormalsCache.normal(i);
            this.bakedColors[i] = this.bakedColorsCache.color(i);
        }
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

    @Override
    public MutableQuad neoBakedNormals(BakedNormals bakedNormals){
        this.bakedNormalsCache = bakedNormals;
        for(int i = 0; i < 4; i++)
            this.bakedNormals[i] = bakedNormals.normal(i);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad neoNormals(float x, float y, float z){
        for(int i = 0; i < 4; i++)
            this.bakedNormals[i] = BakedNormals.pack(x, y, z);
        this.invalidateNeoNormalsCache();
        return this;
    }

    @Override
    public MutableQuad neoNormals(Vector3fc normal){
        this.neoNormals(normal.x(), normal.y(), normal.z());
        return this;
    }

    @Override
    public MutableQuad resetNeoNormals(){
        this.neoBakedNormals(BakedNormals.UNSPECIFIED);
        return this;
    }

    @Override
    public MutableQuad neoNormal(int vertexIndex, float x, float y, float z){
        this.bakedNormals[vertexIndex] = BakedNormals.pack(x, y, z);
        this.invalidateNeoNormalsCache();
        return this;
    }

    @Override
    public MutableQuad neoNormal(int vertexIndex, Vector3fc position){
        this.neoNormal(vertexIndex, position.x(), position.y(), position.z());
        return this;
    }

    @Override
    public BakedNormals neoBakedNormals(){
        if(this.bakedNormalsCache == null)
            this.bakedNormalsCache = BakedNormals.of(this.bakedNormals[0], this.bakedNormals[1], this.bakedNormals[2], this.bakedNormals[3]);
        return this.bakedNormalsCache;
    }

    @Override
    public Vector3fc copyNeoNormal(int vertexIndex, @Nullable Vector3f dest){
        if(dest == null)
            dest = new Vector3f();
        BakedNormals.unpack(this.bakedNormals[vertexIndex], dest);
        return null;
    }

    @Override
    public Vector3fc neoNormal(int vertexIndex){
        return this.copyNeoNormal(vertexIndex, null);
    }

    @Override
    public MutableQuad neoBakedColors(BakedColors bakedColors){
        this.bakedColorsCache = bakedColors;
        for(int i = 0; i < 4; i++)
            this.bakedColors[i] = bakedColors.color(i);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad neoColors(int color){
        for(int i = 0; i < 4; i++)
            this.bakedColors[i] = color;
        this.invalidateNeoColorsCache();
        return this;
    }

    @Override
    public MutableQuad neoColors(float r, float g, float b, float a){
        this.neoColors(ARGB.colorFromFloat(a, r, g, b));
        return this;
    }

    @Override
    public MutableQuad neoColors(float r, float g, float b){
        this.neoColors(r, g, b, 1);
        return this;
    }

    @Override
    public MutableQuad resetNeoColors(){
        this.neoBakedColors(BakedColors.DEFAULT);
        return this;
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, int color){
        this.bakedColors[vertexIndex] = color;
        this.invalidateNeoColorsCache();
        return this;
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, float r, float g, float b, float a){
        this.neoColor(vertexIndex, ARGB.colorFromFloat(a, r, g, b));
        return this;
    }

    @Override
    public MutableQuad neoColor(int vertexIndex, float r, float g, float b){
        this.neoColor(vertexIndex, r, g, b, 1);
        return this;
    }

    @Override
    public BakedColors neoBakedColors(){
        if(this.bakedColorsCache == null)
            this.bakedColorsCache = BakedColors.of(this.bakedColors[0], this.bakedColors[1], this.bakedColors[2], this.bakedColors[3]);
        return this.bakedColorsCache;
    }

    @Override
    public int neoColor(int vertexIndex){
        return this.bakedColors[vertexIndex];
    }

    private void invalidateBakedQuadCache(){
        this.bakedQuadCache = null;
    }

    private void invalidateNeoNormalsCache(){
        this.bakedNormalsCache = null;
        this.invalidateBakedQuadCache();
    }

    private void invalidateNeoColorsCache(){
        this.bakedColorsCache = null;
        this.invalidateBakedQuadCache();
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
            boolean emissive = this.emissive();
            this.bakedQuadCache = new BakedQuad(
                this.position(0), this.position(1), this.position(2), this.position(3),
                UVPair.pack(this.u(0), this.v(0)), UVPair.pack(this.u(1), this.v(1)), UVPair.pack(this.u(2), this.v(2)), UVPair.pack(this.u(3), this.v(3)),
                this.tintIndex,
                this.facing,
                this.sprite,
                !emissive && this.shade(),
                emissive ? 15 : this.lightEmission(),
                this.neoBakedNormals(),
                this.neoBakedColors(),
                !emissive && this.ambientOcclusion()
            );
        }
        return this.bakedQuadCache;
    }
}
