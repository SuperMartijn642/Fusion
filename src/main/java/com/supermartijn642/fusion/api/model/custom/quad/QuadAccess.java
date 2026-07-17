package com.supermartijn642.fusion.api.model.custom.quad;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A view of the properties that make up a quad.
 * <p>
 * Created 30/04/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface QuadAccess {

    /**
     * Creates a new instance representing the properties of the given baked quad.
     */
    static QuadAccess create(BakedQuad quad){
        return MutableQuad.create(quad);
    }

    /**
     * Creates a baked quad instance from the properties.
     */
    BakedQuad toBakedQuad();

    /**
     * Position of the given vertex.
     */
    Vector3fc copyPosition(int vertexIndex, @Nullable Vector3f dest);

    /**
     * Position of the given vertex.
     */
    Vector3fc position(int vertexIndex);

    /**
     * x-position of the given vertex.
     */
    float x(int vertexIndex);

    /**
     * y-position of the given vertex.
     */
    float y(int vertexIndex);

    /**
     * z-position of the given vertex.
     */
    float z(int vertexIndex);

    /**
     * Texture atlas u-position of the given vertex.
     */
    float u(int vertexIndex);

    /**
     * Texture atlas v-position of the given vertex.
     */
    float v(int vertexIndex);

    /**
     * Direction that the quad is facing.
     */
    Direction facing();

    /**
     * Texture atlas sprite of the quad.
     */
    TextureAtlasSprite sprite();

    /**
     * Chunk layer that the quad should be rendered in.
     */
    @Nullable
    ChunkSectionLayer chunkLayer();

    /**
     * Render type that should be used when rendering the quad as an item.
     */
    @Nullable
    RenderType itemRenderType();

    /**
     * Tint index to use for tinting the quad.
     */
    int tintIndex();

    /**
     * Whether the quad should be shaded.
     */
    boolean shade();

    /**
     * Base light-level of the quad.
     */
    int lightEmission();

    /**
     * Whether the quad should be rendered with ambient occlusion.
     */
    boolean ambientOcclusion();

    /**
     * Whether the quad is emissive.
     */
    boolean emissive();

    /**
     * NeoForge's baked normals for the quad.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    BakedNormals neoBakedNormals();

    /**
     * NeoForge's baked normal for the given vertex.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    Vector3fc copyNeoNormal(int vertexIndex, @Nullable Vector3f dest);

    /**
     * NeoForge's baked normal for the given vertex.
     * @see BakedQuad#bakedNormals()
     * @see BakedNormals
     */
    Vector3fc neoNormal(int vertexIndex);

    /**
     * NeoForge's baked colors for the quad.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    BakedColors neoBakedColors();

    /**
     * NeoForge's baked colors for the given vertex.
     * @see BakedQuad#bakedColors()
     * @see BakedColors
     */
    int neoColor(int vertexIndex);
}
