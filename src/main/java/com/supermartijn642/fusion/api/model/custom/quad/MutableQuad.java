package com.supermartijn642.fusion.api.model.custom.quad;

import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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
     * Copies all properties from the given Fabric Rendering API quad view.
     */
    MutableQuad copyFrapiQuad(QuadView quad);

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
    MutableQuad position(int vertexIndex, Vector3f position);

    /**
     * Sets the texture atlas uv-coordinates for the given vertex.
     */
    MutableQuad uv(int vertexIndex, float u, float v);

    /**
     * Sets the material for the quad.
     */
    MutableQuad sprite(TextureAtlasSprite sprite);

    /**
     * Sets chunk layer and item render type to use when rendering the quad.
     */
    MutableQuad renderTypes(@Nullable RenderType chunkRenderType, @Nullable RenderType itemRenderType);

    /**
     * Sets render type to use when rendering the quad in a chunk.
     */
    MutableQuad chunkRenderType(@Nullable RenderType chunkRenderType);

    /**
     * Sets render type to use when rendering the quad as an item.
     */
    MutableQuad itemRenderType(@Nullable RenderType itemRenderType);

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
     * Sets whether the quad is emissive.
     */
    MutableQuad emissive(boolean emissive);
}
