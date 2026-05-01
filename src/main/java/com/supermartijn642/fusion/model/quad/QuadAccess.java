package com.supermartijn642.fusion.model.quad;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3fc;

/**
 * Created 30/04/2026 by SuperMartijn642
 */
public interface QuadAccess {

    BakedQuad toBakedQuad();

    Vector3fc position(int vertexIndex);

    float x(int vertexIndex);

    float y(int vertexIndex);

    float z(int vertexIndex);

    float u(int vertexIndex);

    float v(int vertexIndex);

    Direction facing();

    TextureAtlasSprite sprite();

    ChunkSectionLayer chunkLayer();

    RenderType itemRenderType();

    int tintIndex();

    boolean shade();

    int lightEmission();

    boolean ambientOcclusion();

    boolean emissive();

    BakedNormals neoBakedNormals();

    Vector3fc neoNormal(int vertexIndex);

    BakedColors neoBakedColors();

    int neoColor(int vertexIndex);
}
