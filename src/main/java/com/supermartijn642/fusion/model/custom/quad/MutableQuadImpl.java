package com.supermartijn642.fusion.model.custom.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.extensions.MaterialInfoExtension;
import com.supermartijn642.fusion.util.BakedQuadHelper;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
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
    private BakedQuad.MaterialInfo materialInfoCache;

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
        BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
        this.materialInfoCache = materialInfo;
        this.sprite = materialInfo.sprite();
        this.tintIndex = materialInfo.tintIndex();
        this.flags = 0;
        if(materialInfo.shade())
            this.flags |= (1 << SHADE);
        this.flags |= (materialInfo.lightEmission() << LIGHT_EMISSION);
        if(MaterialInfoExtension.getAmbientOcclusion(materialInfo))
            this.flags |= (1 << AMBIENT_OCCLUSION);
        this.chunkLayer = materialInfo.layer();
        this.itemRenderType = materialInfo.itemRenderType();
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
    public MutableQuad copyMaterialInfo(BakedQuad.MaterialInfo materialInfo){
        this.sprite = materialInfo.sprite();
        this.chunkLayer = materialInfo.layer();
        this.itemRenderType = materialInfo.itemRenderType();
        this.tintIndex = materialInfo.tintIndex();
        this.shade(materialInfo.shade());
        this.lightEmission(materialInfo.lightEmission());
        this.ambientOcclusion(MaterialInfoExtension.getAmbientOcclusion(materialInfo));
        this.materialInfoCache = materialInfo;
        this.invalidateBakedQuadCache();
        return this;
    }

    @Override
    public MutableQuad material(ModelMaterial.Resolved material){
        this.sprite = material.sprite();
        this.renderLayers(material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency());
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad material(Material.Baked material){
        this.sprite = material.sprite();
        this.renderLayers(material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency());
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public MutableQuad sprite(TextureAtlasSprite sprite){
        this.sprite = sprite;
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public TextureAtlasSprite sprite(){
        return this.sprite;
    }

    @Override
    public MutableQuad renderLayers(Transparency transparency){
        this.chunkLayer = ChunkSectionLayer.byTransparency(transparency);
        this.itemRenderType = null;
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
        if(this.chunkLayer == null && this.sprite != null)
            return ChunkSectionLayer.byTransparency(this.sprite.transparency());
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
            Transparency transparency = this.chunkLayer == null ?
                this.sprite.transparency() :
                this.chunkLayer == ChunkSectionLayer.TRANSLUCENT ? Transparency.TRANSPARENT_AND_TRANSLUCENT : this.chunkLayer == ChunkSectionLayer.CUTOUT ? Transparency.TRANSPARENT : Transparency.NONE;
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
        this.flags = shade ? this.flags | (1 << SHADE) : this.flags & ~(1 << SHADE);
        this.invalidateMaterialInfoCache();
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
        this.invalidateMaterialInfoCache();
        return this;
    }

    @Override
    public int lightEmission(){
        return (this.flags >> LIGHT_EMISSION) & 15;
    }

    @Override
    public MutableQuad ambientOcclusion(boolean ambientOcclusion){
        this.flags = ambientOcclusion ? this.flags | (1 << AMBIENT_OCCLUSION) : this.flags & ~(1 << AMBIENT_OCCLUSION);
        this.invalidateMaterialInfoCache();
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

    private void invalidateMaterialInfoCache(){
        this.materialInfoCache = null;
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
            if(this.materialInfoCache == null){
                if(this.sprite == null)
                    throw new IllegalStateException("No sprite was specified!");
                ChunkSectionLayer chunkLayer = this.chunkLayer;
                RenderType itemRenderType = this.itemRenderType;
                if(chunkLayer == null)
                    chunkLayer = ChunkSectionLayer.byTransparency(this.sprite.transparency());
                if(itemRenderType == null){
                    Transparency transparency = this.chunkLayer == null ?
                        this.sprite.transparency() :
                        this.chunkLayer == ChunkSectionLayer.TRANSLUCENT ? Transparency.TRANSPARENT_AND_TRANSLUCENT : this.chunkLayer == ChunkSectionLayer.CUTOUT ? Transparency.TRANSPARENT : Transparency.NONE;
                    itemRenderType = TextureAtlas.LOCATION_BLOCKS.equals(this.sprite.atlasLocation()) ?
                        transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet() :
                        transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
                }
                boolean emissive = this.emissive();
                this.materialInfoCache = new BakedQuad.MaterialInfo(
                    this.sprite,
                    chunkLayer, itemRenderType,
                    this.tintIndex,
                    !emissive && this.shade(),
                    emissive ? 15 : this.lightEmission()
                );
                MaterialInfoExtension.setAmbientOcclusion(this.materialInfoCache, !emissive && this.ambientOcclusion());
            }
            this.bakedQuadCache = new BakedQuad(
                this.position(0), this.position(1), this.position(2), this.position(3),
                UVPair.pack(this.u(0), this.v(0)), UVPair.pack(this.u(1), this.v(1)), UVPair.pack(this.u(2), this.v(2)), UVPair.pack(this.u(3), this.v(3)),
                this.facing,
                this.materialInfoCache
            );
        }
        return this.bakedQuadCache;
    }
}
