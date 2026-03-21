package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Consumer;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
public interface TextureOutput<X> {

    /**
     * Begins a new sprite builder.
     * A sprite builder must be submitted through {@link SpriteBuilder#submit()} before a new sprite can be started.
     */
    SpriteBuilder createSprite();

    /**
     * Sets custom data that can be retrieved from the sprites through {@link SpriteHelper#getTextureInstance(TextureAtlasSprite)}.
     * @param customData arbitrary data
     */
    void setCustomData(X customData);

    /**
     * Sets the callback that is called after the sprites have been created and stitched.
     */
    void setCreationCallback(Consumer<TextureInstance<X>> callback);
}
