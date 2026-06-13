package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.resources.Identifier;
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
     * Gets the atlas which the sprite is stitched to.
     */
    Identifier getAtlasLocation();

    /**
     * Gets the padding of the texture atlas sprite.
     */
    int getSpritePadding();

    /**
     * Gets the configured number of mipmap levels.
     */
    int getMipmapLevels();
}
