package com.supermartijn642.fusion.api.texture;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface SpriteCreationContext {

    /**
     * Creates the sprite as if it were created by vanilla.
     * Note that if this is called, responsibility for closing the returned sprite's resources lies with the caller.
     */
    TextureAtlasSprite createOriginalSprite();

    /**
     * Gets the width of the texture.
     */
    int getTextureWidth();

    /**
     * Gets the height of the texture.
     */
    int getTextureHeight();

    /**
     * Gets the identifier of the texture.
     */
    ResourceLocation getTextureIdentifier();

    /**
     * Gets the texture data. Each element in the array corresponds to one mipmap level.
     * Note that if this is called, responsibility for closing the returned images lies with the caller.
     */
    NativeImage[] getTextureBuffers();

    /**
     * Gets the width of the atlas.
     */
    int getAtlasWidth();

    /**
     * Gets the height of the texture.
     */
    int getAtlasHeight();

    /**
     * Gets the atlas which the sprite is stitched to.
     */
    AtlasTexture getAtlas();

    /**
     * Gets the x-position of the sprite on the atlas.
     */
    int getSpritePositionX();

    /**
     * Gets the y-position of the sprite on the atlas.
     */
    int getSpritePositionY();

    /**
     * Gets the width of the sprite as allocated on the atlas.
     */
    int getSpriteWidth();

    /**
     * Gets the height of the sprite as allocated on the atlas.
     */
    int getSpriteHeight();

    /**
     * Gets the configured number of mipmap levels.
     */
    int getMipmapLevels();
}
