package com.supermartijn642.fusion.model.quad;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3fc;

/**
 * Created 30/04/2026 by SuperMartijn642
 */
public interface MutableQuad extends QuadAccess {

    MutableQuad copyFrom(QuadAccess quad);

    MutableQuad copyBakedQuad(BakedQuad quad);

    MutableQuad createCopy();

    MutableQuad position(int vertexIndex, float x, float y, float z);

    MutableQuad position(int vertexIndex, Vector3fc position);

    MutableQuad uv(int vertexIndex, float u, float v);

    MutableQuad copyMaterialInfo(BakedQuad.MaterialInfo materialInfo);

    MutableQuad material(Material.Baked material);

    MutableQuad sprite(TextureAtlasSprite sprite, boolean copyTransparency);

    MutableQuad sprite(TextureAtlasSprite sprite);

    MutableQuad transparency(Transparency transparency);

    MutableQuad renderLayers(ChunkSectionLayer chunkLayer, RenderType itemRenderType);

    MutableQuad chunkLayer(ChunkSectionLayer chunkLayer);

    MutableQuad itemRenderType(RenderType itemRenderType);

    MutableQuad tintIndex(int tintIndex);

    MutableQuad shade(boolean shade);

    MutableQuad lightEmission(int lightEmission);

    MutableQuad ambientOcclusion(boolean ambientOcclusion);

    MutableQuad emissive(boolean emissive);

    MutableQuad neoBakedNormals(BakedNormals bakedNormals);

    MutableQuad neoNormals(float x, float y, float z);

    MutableQuad neoNormals(Vector3fc normal);

    MutableQuad resetNeoNormals();

    MutableQuad neoNormal(int vertexIndex, float x, float y, float z);

    MutableQuad neoNormal(int vertexIndex, Vector3fc position);

    MutableQuad neoBakedColors(BakedColors bakedColors);

    MutableQuad neoColors(int color);

    MutableQuad neoColors(float r, float g, float b, float a);

    MutableQuad neoColors(float r, float g, float b);

    MutableQuad resetNeoColors();

    MutableQuad neoColor(int vertexIndex, int color);

    MutableQuad neoColor(int vertexIndex, float r, float g, float b, float a);

    MutableQuad neoColor(int vertexIndex, float r, float g, float b);
}
