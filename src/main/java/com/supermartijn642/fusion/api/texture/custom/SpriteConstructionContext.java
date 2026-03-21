package com.supermartijn642.fusion.api.texture.custom;


import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
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
    TextureAtlas getTextureAtlas();

    /**
     * Gets the configured number of mipmap levels.
     */
    int getMipmapLevels();
}
