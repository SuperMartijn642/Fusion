package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface SpriteBuilder {

    /**
     * Sets the name for this sprite.
     * The name will be used as a suffix to the texture identifier when a texture submits multiple sprites.
     */
    SpriteBuilder name(String name);

    /**
     * Sets the image to be used for this sprite. The given image also determines the sprites size.
     */
    SpriteBuilder image(SpriteImageSource image);

    /**
     * Allows custom creation of the {@link TextureAtlasSprite} instance.
     * @param width  width of the sprite
     * @param height height of the sprite
     */
    SpriteBuilder customConstructor(int width, int height, Constructor constructor);

    /**
     * Marks the sprite as the default one.
     * The default sprite will use the original texture identifier and show up when no further processing is done.
     * Only one sprite can be marked as default. If no sprite is marked, then the first submitted sprite will be the default.
     */
    SpriteBuilder markDefaultSprite();

    /**
     * Sets whether the sprite is marked as the default one.
     * The default sprite will use the original texture identifier and show up when no further processing is done.
     * Only one sprite can be marked as default. If no sprite is marked, then the first submitted sprite will be the default.
     */
    SpriteBuilder markDefaultSprite(boolean markDefault);

    /**
     * Sets the callback that is called after the sprite has been created and stitched.
     */
    SpriteBuilder setCreationCallback(Consumer<SpriteInstance> callback);

    /**
     * Finalizes this sprite. After submission, this builder can no longer be accessed.
     */
    void submit();

    interface Constructor {
        TextureAtlasSprite create(AllocatedSprite sprite, SpriteConstructionContext context);
    }
}
