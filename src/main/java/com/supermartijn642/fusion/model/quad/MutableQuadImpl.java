package com.supermartijn642.fusion.model.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.extensions.MaterialInfoExtension;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Created 11/09/2024 by SuperMartijn642
 */
public class MutableQuadImpl implements MutableQuad {

    // Quad data
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

    public MutableQuadImpl(){
        for(int i = 0; i < 4; i++)
            this.positions[i] = new Vector3f();
    }

    @Override
    public MutableQuad copyFrom(QuadAccess quad){
        // Quad data
        for(int i = 0; i < 4; i++){
            this.positions[i].set(quad.position(i));
            this.uvs[i] = UVPair.pack(quad.u(i), quad.v(i));
        }
        this.facing = quad.facing();
        // Material data
        this.materialInfoCache = null;
        this.sprite = quad.sprite();
        this.chunkLayer = quad.chunkLayer();
        this.itemRenderType = quad.itemRenderType();
        this.tintIndex = quad.tintIndex();
        this.shade = quad.shade();
        this.lightEmission = quad.lightEmission();
        // Our properties
        this.ambientOcclusion = quad.ambientOcclusion();
        this.emissive = quad.emissive();
        return this;
    }

    @Override
    public MutableQuad copyBakedQuad(BakedQuad quad){
        // Quad data
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
        this.ambientOcclusion = MaterialInfoExtension.getAmbientOcclusion(materialInfo);
        this.emissive = false;
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
        return this;
    }

    @Override
    public MutableQuad position(int vertexIndex, Vector3fc position){
        this.positions[vertexIndex].set(position);
        this.facing = null;
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
        this.ambientOcclusion = MaterialInfoExtension.getAmbientOcclusion(materialInfo);
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
        this.sprite(sprite, true);
        return this;
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

    private void invalidateMaterialInfoCache(){
        this.materialInfoCache = null;
    }

    public BakedQuad toBakedQuad(){
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
                this.emissive || this.shade,
                this.emissive ? 15 : this.lightEmission
            );
            MaterialInfoExtension.setAmbientOcclusion(this.materialInfoCache, !this.emissive && this.ambientOcclusion);
        }
        return new BakedQuad(
            new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
            this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
            this.facing,
            this.materialInfoCache
        );
    }
}
