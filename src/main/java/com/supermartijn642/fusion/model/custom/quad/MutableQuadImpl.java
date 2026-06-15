package com.supermartijn642.fusion.model.custom.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.LightCoordsUtil;
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
        this.ambientOcclusion = true;
        this.emissive = false;
        return this;
    }

    @Override
    public MutableQuad copyFrapiQuad(QuadView quad){
        // Quad data
        this.bakedQuadCache = null;
        for(int i = 0; i < 4; i++){
            quad.copyPos(i, this.positions[i]);
            this.uvs[i] = UVPair.pack(quad.u(i), quad.v(i));
        }
        this.facing = null;
        // Material data
        this.materialInfoCache = null;
        this.sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId()).spriteFinder().find(quad);
        this.chunkLayer = quad.chunkLayer();
        this.itemRenderType = quad.itemRenderType();
        this.tintIndex = quad.tintIndex();
        this.shade = quad.diffuseShade();
        this.lightEmission = 15;
        for(int i = 0; i < 4; i++){
            int lightmap = quad.lightmap(i);
            if(lightmap == 0){
                this.lightEmission = 0;
                break;
            }
            int blockLight = LightCoordsUtil.block(lightmap);
            int skyLight = LightCoordsUtil.sky(lightmap);
            this.lightEmission = Math.min(this.lightEmission, Math.min(blockLight, skyLight));
        }
        // Our properties
        this.ambientOcclusion = quad.ambientOcclusion() != TriState.FALSE;
        this.emissive = quad.emissive();
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
                this.facing = FaceBakery.calculateFacing(this.positions);
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
                this.materialInfoCache = new BakedQuad.MaterialInfo(
                    this.sprite,
                    chunkLayer, itemRenderType,
                    this.tintIndex,
                    !this.emissive && this.shade,
                    this.emissive ? 15 : this.lightEmission
                );
            }
            this.bakedQuadCache = new BakedQuad(
                new Vector3f(this.positions[0]), new Vector3f(this.positions[1]), new Vector3f(this.positions[2]), new Vector3f(this.positions[3]),
                this.uvs[0], this.uvs[1], this.uvs[2], this.uvs[3],
                this.facing,
                this.materialInfoCache
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
            quad.pos(i, this.positions[i]);
            quad.uv(i, UVPair.unpackU(this.uvs[i]), UVPair.unpackV(this.uvs[i]));
        }
        quad.color(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);

        int lightmap = LightCoordsUtil.pack(this.lightEmission, this.lightEmission);
        quad.lightmap(lightmap, lightmap, lightmap, lightmap);

        if(this.facing == null){
            this.facing = FaceBakery.calculateFacing(this.positions);
            if(this.facing == null)
                this.facing = Direction.UP;
        }
        for(int i = 0; i < 4; i++)
            quad.normal(0, this.facing.getStepX(), this.facing.getStepY(), this.facing.getStepZ());
        quad.nominalFace(this.facing);


        QuadAtlas atlas = QuadAtlas.ofLocation(this.sprite.atlasLocation());
        if(atlas == null)
            atlas = QuadAtlas.BLOCK;
        quad.atlas(atlas);

        ChunkSectionLayer chunkLayer = this.chunkLayer;
        if(chunkLayer == null)
            chunkLayer = ChunkSectionLayer.byTransparency(this.sprite.transparency());
        quad.chunkLayer(chunkLayer);
        RenderType itemRenderType = this.itemRenderType;
        if(itemRenderType == null){
            Transparency transparency = this.chunkLayer == null ?
                this.sprite.transparency() :
                this.chunkLayer == ChunkSectionLayer.TRANSLUCENT ? Transparency.TRANSPARENT_AND_TRANSLUCENT : this.chunkLayer == ChunkSectionLayer.CUTOUT ? Transparency.TRANSPARENT : Transparency.NONE;
            itemRenderType = TextureAtlas.LOCATION_BLOCKS.equals(this.sprite.atlasLocation()) ?
                transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet() :
                transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet();
        }
        quad.itemRenderType(itemRenderType);
        quad.animated(this.sprite.contents().isAnimated());
        quad.tintIndex(this.tintIndex);
        quad.diffuseShade(!this.emissive && this.shade);
        quad.ambientOcclusion(!this.emissive && this.ambientOcclusion ? TriState.TRUE : TriState.FALSE);
        quad.emissive(this.emissive);
    }
}
