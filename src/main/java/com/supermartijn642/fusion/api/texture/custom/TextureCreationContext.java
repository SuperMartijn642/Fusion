package com.supermartijn642.fusion.api.texture.custom;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface TextureCreationContext {

    /**
     * Gets the identifier of the texture.
     */
    Identifier getIdentifier();

    /**
     * Gets the image loaded from the texture file.
     * Note that if this is called, responsibility for closing the image lies with the caller.
     */
    NativeImage getImage();

    /**
     * Gets the width of the texture.
     */
    int getImageWidth();

    /**
     * Gets the height of the texture.
     */
    int getImageHeight();

    /**
     * Gets the vanilla animation metadata for the texture.
     */
    @Nullable AnimationMetadataSection getAnimationMetadata();
}
