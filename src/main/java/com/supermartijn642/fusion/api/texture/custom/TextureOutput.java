package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.util.UserErrorException;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Created 22/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
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

    /**
     * Creates a sub-texture for the given raw texture instance.
     * The sub-texture must be submitted through the returned instance.
     * @param texture           raw texture to be created
     * @param name              name of the sub-texture
     * @param image             image for the texture
     * @param animationMetadata vanilla animation metadata
     * @throws UserErrorException when the texture creation throws a user error
     */
    <Y> SubTextureOutput<Y> createSubTexture(RawTextureInstance<?,Y> texture,
                                             @Nullable String name,
                                             BufferedImage image,
                                             @Nullable AnimationMetadataSection animationMetadata) throws UserErrorException;

    /**
     * Helper for submitting sub-textures.
     */
    @ApiStatus.NonExtendable
    interface SubTextureOutput<X> {
        /**
         * Marks the sub-texture as the default one.
         * The default sprite or sub-texture will use the original texture identifier and show up when no further processing is done.
         * Only one sprite or sub-texture can be marked as default. If no sprite is marked, then the first submitted sprite will be the default.
         */
        default SubTextureOutput<X> markDefault(){
            return this.markDefault(true);
        }

        /**
         * Sets whether the sub-texture is marked as the default one.
         * The default sprite or sub-texture will use the original texture identifier and show up when no further processing is done.
         * Only one sprite or sub-texture can be marked as default. If no sprite is marked, then the first submitted sprite will be the default.
         */
        SubTextureOutput<X> markDefault(boolean markDefault);

        /**
         * Sets the callback that is called after the sub-texture sprites have been created and stitched.
         */
        SubTextureOutput<X> setCreationCallback(Consumer<TextureInstance<X>> callback);

        /**
         * Finalizes this sub-texture. After submission, this builder can no longer be accessed.
         */
        void submit();
    }
}
