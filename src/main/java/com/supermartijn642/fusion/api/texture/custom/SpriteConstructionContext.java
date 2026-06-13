package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.client.renderer.texture.AtlasTexture;
import org.jetbrains.annotations.ApiStatus;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface SpriteConstructionContext {

    /**
     * Gets the width of the atlas.
     */
    int getAtlasWidth();

    /**
     * Gets the height of the texture.
     */
    int getAtlasHeight();

    /**
     * Gets the atlas that the sprite is stitched to.
     */
    AtlasTexture getTextureAtlas();

    /**
     * Gets the configured number of mipmap levels.
     */
    int getMipmapLevels();
}
