package com.supermartijn642.fusion.api.model.custom.quad;

import com.mojang.math.Vector3f;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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
     * Copies the properties to a Fabric Rendering API quad view.
     */
    void toFrapiQuad(MutableQuadView quad);

    /**
     * Position of the given vertex.
     */
    Vector3f copyPosition(int vertexIndex, @Nullable Vector3f dest);

    /**
     * Position of the given vertex.
     */
    Vector3f position(int vertexIndex);

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
     * Render type that should be used when rendering the quad in a chunk.
     */
    @Nullable
    RenderType chunkRenderType();

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
     * Whether the quad is emissive.
     */
    boolean emissive();
}
