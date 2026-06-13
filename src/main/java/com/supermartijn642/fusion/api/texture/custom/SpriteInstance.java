package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface SpriteInstance {

    /**
     * Gets the texture instance this sprite belongs to.
     */
    TextureInstance<?> getTexture();

    /**
     * Gets the vanilla texture atlas instance associated with this sprite.
     */
    TextureAtlasSprite getSprite();

    /**
     * Gets the identifier of this sprite.
     */
    Identifier getIdentifier();

    /**
     * Gets min u-coordinate of the sprite on the atlas.
     */
    float getU0();

    /**
     * Gets max u-coordinate of the sprite on the atlas.
     */
    float getU1();

    /**
     * Gets min v-coordinate of the sprite on the atlas.
     */
    float getV0();

    /**
     * Gets max v-coordinate of the sprite on the atlas.
     */
    float getV1();
}
