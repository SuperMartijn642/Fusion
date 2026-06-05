package com.supermartijn642.fusion.model.custom.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
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
    // Material data
    private BakedQuad.MaterialInfo materialInfoCache;
    private TextureAtlasSprite sprite;
    private ChunkSectionLayer chunkLayer;
    private RenderType itemRenderType;
    private Transparency transparency;
    private int tintIndex = -1;
    private boolean shade = true;
    private int lightEmission = 0;
    // Our properties
    private boolean ambientOcclusion = true;
    private boolean emissive = false;
    // NeoForge data
    private BakedNormals bakedNormalsCache = BakedNormals.UNSPECIFIED;
    private final Vector3f[] bakedNormals = new Vector3f[4];
    private BakedColors bakedColorsCache = BakedColors.DEFAULT;
    private final int[] bakedColors = new int[4];

    public MutableQuadImpl(){
        for(int i = 0; i < 4; i++){
            this.positions[i] = new Vector3f();
            this.bakedNormals[i] = new Vector3f();
        }
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
        // Material data
        this.materialInfoCache = impl.materialInfoCache;
        this.sprite = impl.sprite;
        this.chunkLayer = impl.chunkLayer;
        this.itemRenderType = impl.itemRenderType;
        this.tintIndex = impl.tintIndex;
        this.shade = impl.shade;
        this.lightEmission = impl.lightEmission;
        // Our properties
        this.ambientOcclusion = impl.ambientOcclusion;
        this.emissive = impl.emissive;
        // NeoForge data
        this.bakedNormalsCache = impl.bakedNormalsCache;
        this.bakedColorsCache = impl.bakedColorsCache;
        for(int i = 0; i < 4; i++)
            this.bakedNormals[i].set(impl.bakedNormals[i]);
        System.arraycopy(impl.bakedColors, 0, this.bakedColors, 0, 4);
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
        // Material data
        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        this.materialInfoCache = materialInfo;
        this.sprite = materialInfo.sprite();
        this.chunkLayer = materialInfo.layer();
        this.itemRenderType = materialInfo.itemRenderType();
        this.tintIndex = materialInfo.tintIndex();
        this.shade = materialInfo.shade();
        this.lightEmission = materialInfo.lightEmission();
        // Our properties
        this.ambientOcclusion = materialInfo.ambientOcclusion();
        this.emissive = false;
        // NeoForge data
        this.bakedNormalsCache = quad.bakedNormals();
        this.bakedColorsCache = quad.bakedColors();
        for(int i = 0; i < 4; i++){
            BakedNormals.unpack(quad.bakedNormals().normal(i), this.bakedNormals[i]);
            this.bakedColors[i] = quad.bakedColors().color(i);
        }
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
    public MutableQuad copyMaterialInfo(BakedQuad.MaterialInfo materialInfo){
        this.materialInfoCache = materialInfo;
        this.sprite = materialInfo.sprite();
        this.chunkLayer = materialInfo.layer();
        this.itemRenderType = materialInfo.itemRenderType();
        this.tintIndex = materialInfo.tintIndex();
        this.shade = materialInfo.shade();
        this.lightEmission = materialInfo.lightEmission();
        this.ambientOcclusion = materialInfo.ambientOcclusion();
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad material(ModelMaterial.Resolved material){
        this.sprite = material.sprite();
        this.transparency(material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency());
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad material(Material.Baked material){
        this.sprite = material.sprite();
        this.transparency(material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency());
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad sprite(TextureAtlasSprite sprite, boolean copyTransparency){
        this.sprite = sprite;
        if(copyTransparency)
            this.transparency(sprite.transparency());
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad sprite(TextureAtlasSprite sprite){
        return this.sprite(sprite, true);
    }

    @Override
    public TextureAtlasSprite sprite(){
        return this.sprite;
    }

    @Override
    public MutableQuad transparency(Transparency transparency){
        this.chunkLayer = null;
        this.itemRenderType = null;
        this.transparency = transparency;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad renderLayers(ChunkSectionLayer chunkLayer, RenderType itemRenderType){
        this.chunkLayer = chunkLayer;
        this.itemRenderType = itemRenderType;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad chunkLayer(ChunkSectionLayer chunkLayer){
        this.chunkLayer = chunkLayer;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public ChunkSectionLayer chunkLayer(){
        if(this.chunkLayer == null){
            if(this.transparency != null)
                return ChunkSectionLayer.byTransparency(this.transparency);
            else if(this.sprite != null)
                return ChunkSectionLayer.byTransparency(this.sprite.transparency());
        }
        this.invalidateMaterialInfoCache();
        return this.chunkLayer;
    }

    @Override
    public MutableQuad itemRenderType(RenderType itemRenderType){
        this.itemRenderType = itemRenderType;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public RenderType itemRenderType(){
        if(this.itemRenderType == null && this.sprite != null){
            Transparency transparency = this.transparency == null ? this.sprite.transparency() : this.transparency;
            return TextureAtlas.LOCATION_BLOCKS.equals(this.sprite.atlasLocation()) ?
                transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet() :
                transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
        }
        return this.itemRenderType;
    }

    @Override
    public MutableQuad tintIndex(int tintIndex){
        this.tintIndex = tintIndex;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public int tintIndex(){
        return this.tintIndex;
    }

    @Override
    public MutableQuad shade(boolean shade){
        this.shade = shade;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public boolean shade(){
        return this.shade;
    }

    @Override
    public MutableQuad lightEmission(int lightEmission){
        this.lightEmission = lightEmission;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public int lightEmission(){
        return this.lightEmission;
    }

    @Override
    public MutableQuad ambientOcclusion(boolean ambientOcclusion){
        this.ambientOcclusion = ambientOcclusion;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public boolean ambientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public MutableQuad emissive(boolean emissive){
        this.emissive = emissive;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public boolean emissive(){
        return this.emissive;
    }

    @Override
    public MutableQuad neoBakedNormals(BakedNormals bakedNormals){
        this.bakedNormalsCache = bakedNormals;
        for(int i = 0; i < 4; i++)
            BakedNormals.unpack(bakedNormals.normal(i), this.bakedNormals[i]);
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad neoNormals(float x, float y, float z){
        for(int i = 0; i < 4; i++)
            this.bakedNormals[i].set(x, y, z);
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
        this.bakedNormals[vertexIndex].set(x, y, z);
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
            this.bakedNormalsCache = BakedNormals.of(
                BakedNormals.pack(this.bakedNormals[0]),
                BakedNormals.pack(this.bakedNormals[1]),
                BakedNormals.pack(this.bakedNormals[2]),
                BakedNormals.pack(this.bakedNormals[3])
            );
        return this.bakedNormalsCache;
    }

    @Override
    public Vector3fc neoNormal(int vertexIndex){
        return this.bakedNormals[vertexIndex];
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

    private void invalidateMaterialInfoCache(){
        this.materialInfoCache = null;
        this.invalidateBakedQuadCache();
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
                this.facing = FaceBakery.calculateFacing(this.positions);
                if(this.facing == null)
                    this.facing = Direction.UP;
            }
            if(this.materialInfoCache == null){
                if(this.sprite == null)
                    throw new IllegalStateException("No sprite was specified!");
                Transparency transparency = this.transparency == null ? this.sprite.transparency() : this.transparency;
                if(this.chunkLayer == null)
                    this.chunkLayer = ChunkSectionLayer.byTransparency(transparency);
                if(this.itemRenderType == null)
                    this.itemRenderType = TextureAtlas.LOCATION_BLOCKS.equals(this.sprite.atlasLocation()) ?
                        transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet() :
                        transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
                this.materialInfoCache = new BakedQuad.MaterialInfo(
                    this.sprite,
                    this.chunkLayer, this.itemRenderType,
                    this.tintIndex,
                    !this.emissive && this.shade,
                    this.emissive ? 15 : this.lightEmission,
                    !this.emissive && this.ambientOcclusion
                );
            }
            if(this.bakedNormalsCache == null)
                this.bakedNormalsCache = BakedNormals.of(
                    BakedNormals.pack(this.bakedNormals[0]),
                    BakedNormals.pack(this.bakedNormals[1]),
                    BakedNormals.pack(this.bakedNormals[2]),
                    BakedNormals.pack(this.bakedNormals[3])
                );
            if(this.bakedColorsCache == null)
                this.bakedColorsCache = BakedColors.of(this.bakedColors[0], this.bakedColors[1], this.bakedColors[2], this.bakedColors[3]);
            this.bakedQuadCache = new BakedQuad(
                new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
                this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
                this.facing,
                this.materialInfoCache,
                this.bakedNormalsCache,
                this.bakedColorsCache
            );
        }
        return this.bakedQuadCache;
    }
}
