package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface AllocatedSprite {

    /**
     * The identifier given to the sprite.
     */
    ResourceLocation identifier();

    /**
     * The x-coordinate of the sprite on the atlas in pixels.
     */
    int x();

    /**
     * The y-coordinate of the sprite on the atlas in pixels.
     */
    int y();

    /**
     * The width of the sprite on the atlas in pixels
     */
    int width();

    /**
     * The height of the sprite on the atlas in pixels
     */
    int height();

    /**
     * The min u-coordinate of the sprite on the atlas.
     */
    float u0();

    /**
     * The max u-coordinate of the sprite on the atlas.
     */
    float u1();

    /**
     * The min v-coordinate of the sprite on the atlas.
     */
    float v0();

    /**
     * The max v-coordinate of the sprite on the atlas.
     */
    float v1();
}
