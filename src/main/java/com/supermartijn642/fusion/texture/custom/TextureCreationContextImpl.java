package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class TextureCreationContextImpl implements TextureCreationContext {

    private final ResourceLocation identifier;
    private final BufferedImage image;
    private final AnimationMetadataSection animationMetadata;

    public TextureCreationContextImpl(ResourceLocation identifier, BufferedImage image, AnimationMetadataSection animationMetadata){
        this.identifier = identifier;
        this.image = image;
        this.animationMetadata = animationMetadata;
    }

    @Override
    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    @Override
    public BufferedImage getImage(){
        return this.image;
    }

    @Override
    public int getImageWidth(){
        return this.image.getWidth();
    }

    @Override
    public int getImageHeight(){
        return this.image.getHeight();
    }

    @Override
    @Nullable
    public AnimationMetadataSection getAnimationMetadata(){
        return this.animationMetadata;
    }
}
