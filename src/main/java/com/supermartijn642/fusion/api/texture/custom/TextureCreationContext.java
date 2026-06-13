package com.supermartijn642.fusion.api.texture.custom;

import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface TextureCreationContext {

    /**
     * Gets the identifier of the texture.
     */
    ResourceLocation getIdentifier();

    /**
     * Gets the image loaded from the texture file.
     */
    BufferedImage getImage();

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
    @Nullable
    AnimationMetadataSection getAnimationMetadata();
}
