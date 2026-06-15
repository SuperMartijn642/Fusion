package com.supermartijn642.fusion.api.model.custom.quad;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3fc;

/**
 * A mutable instance of the properties that make up a quad.
 * <p>
 * Created 30/04/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface MutableQuad extends QuadAccess {

    /**
     * Creates a new instance of mutable quad properties.
     */
    static MutableQuad create(){
        return MutableQuadImpl.create();
    }

    /**
     * Creates a new mutable instance representing the properties of the given baked quad.
     */
    static MutableQuad create(BakedQuad quad){
        return create().copyBakedQuad(quad);
    }

    /**
     * Copies all properties from the given quad.
     */
    MutableQuad copyFrom(QuadAccess quad);

    /**
     * Copies all properties from the given baked quad.
     */
    MutableQuad copyBakedQuad(BakedQuad quad);

    /**
     * Creates a copy of this set of quad properties.
     */
    MutableQuad createCopy();

    /**
     * Sets the position for the given vertex.
     */
    MutableQuad position(int vertexIndex, float x, float y, float z);

    /**
     * Sets the position for the given vertex.
     */
    MutableQuad position(int vertexIndex, Vector3fc position);

    /**
     * Sets the texture atlas uv-coordinates for the given vertex.
     */
    MutableQuad uv(int vertexIndex, float u, float v);

    /**
     * Copies the properties of the given material info.
     */
    MutableQuad copyMaterialInfo(BakedQuad.MaterialInfo materialInfo);

    /**
     * Sets the material for the quad.
     */
    MutableQuad material(ModelMaterial.Resolved material);

    /**
     * Sets the material for the quad.
     */
    MutableQuad material(Material.Baked material);

    /**
     * Sets the material for the quad.
     */
    MutableQuad sprite(TextureAtlasSprite sprite);

    /**
     * Sets chunk layer and item render type to those associated with the given transparency.
     */
    MutableQuad renderLayers(Transparency transparency);

    /**
     * Sets chunk layer and item render type to use when rendering the quad.
     */
    MutableQuad renderLayers(ChunkSectionLayer chunkLayer, RenderType itemRenderType);

    /**
     * Sets chunk layer to use when rendering the quad.
     */
    MutableQuad chunkLayer(ChunkSectionLayer chunkLayer);

    /**
     * Sets render type to use when rendering the quad as an item.
     */
    MutableQuad itemRenderType(RenderType itemRenderType);

    /**
     * Sets the tint index to use for tinting the quad.
     */
    MutableQuad tintIndex(int tintIndex);

    /**
     * Sets whether the quad should be shaded.
     */
    MutableQuad shade(boolean shade);

    /**
     * Sets the base light-level for the quad.
     * The given value should be in the range 0..15.
     */
    MutableQuad lightEmission(int lightEmission);

    /**
     * Sets whether the quad should be rendered with ambient occlusion.
     */
    MutableQuad ambientOcclusion(boolean ambientOcclusion);

    /**
     * Sets whether the quad is emissive.
     */
    MutableQuad emissive(boolean emissive);

    /**
     * Sets NeoForge's baked normals for the quad.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad neoBakedNormals(BakedNormals bakedNormals);

    /**
     * Sets NeoForge's baked normals for the quad.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad neoNormals(float x, float y, float z);

    /**
     * Sets NeoForge's baked normals for the quad.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad neoNormals(Vector3fc normal);

    /**
     * Sets NeoForge's baked normals for the quad to the default values.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad resetNeoNormals();

    /**
     * Sets NeoForge's baked normal for the given vertex.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad neoNormal(int vertexIndex, float x, float y, float z);

    /**
     * Sets NeoForge's baked normal for the given vertex.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    MutableQuad neoNormal(int vertexIndex, Vector3fc position);

    /**
     * Sets NeoForge's baked colors for the quad.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoBakedColors(BakedColors bakedColors);

    /**
     * Sets NeoForge's baked colors for the quad.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColors(int color);

    /**
     * Sets NeoForge's baked colors for the quad.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColors(float r, float g, float b, float a);

    /**
     * Sets NeoForge's baked colors for the quad.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColors(float r, float g, float b);

    /**
     * Sets NeoForge's baked colors for the quad to the default values.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad resetNeoColors();

    /**
     * Sets NeoForge's baked color for the given vertex.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColor(int vertexIndex, int color);

    /**
     * Sets NeoForge's baked color for the given vertex.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColor(int vertexIndex, float r, float g, float b, float a);

    /**
     * Sets NeoForge's baked color for the given vertex.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    MutableQuad neoColor(int vertexIndex, float r, float g, float b);
}
