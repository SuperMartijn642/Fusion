package com.supermartijn642.fusion.api.texture.custom;


import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public interface TextureCreationContext {

    /**
     * Gets the identifier of the texture.
     */
    ResourceLocation getIdentifier();

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
    @Nullable
    AnimationMetadataSection getAnimationMetadata();
}
