package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class SpritePreparationContextImpl implements SpritePreparationContext {

    private final int originalWidth, originalHeight;
    private final int textureWidth, textureHeight;
    private final ResourceLocation identifier;
    private final AnimationMetadataSection animationMetadata;

    public SpritePreparationContextImpl(int originalWidth, int originalHeight, int textureWidth, int textureHeight, ResourceLocation identifier, AnimationMetadataSection animationMetadata){
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.identifier = identifier;
        this.animationMetadata = animationMetadata == AnimationMetadataSection.EMPTY ? null : animationMetadata;
    }

    @Override
    public int getOriginalFrameWith(){
        return this.originalWidth;
    }

    @Override
    public int getOriginalFrameHeight(){
        return this.originalHeight;
    }

    @Override
    public int getTextureWidth(){
        return this.textureWidth;
    }

    @Override
    public int getTextureHeight(){
        return this.textureHeight;
    }

    @Override
    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    @Override
    @Nullable
    public AnimationMetadataSection getAnimationMetadata(){
        return this.animationMetadata;
    }
}
