package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.TextureCreationContext;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class TextureCreationContextImpl implements TextureCreationContext, AutoCloseable {

    private final ResourceLocation identifier;
    private final NativeImage image;
    private final AnimationMetadataSection animationMetadata;

    private boolean imageRequested = false;

    public TextureCreationContextImpl(ResourceLocation identifier, NativeImage image, AnimationMetadataSection animationMetadata){
        this.identifier = identifier;
        this.image = image;
        this.animationMetadata = animationMetadata;
    }

    @Override
    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    @Override
    public NativeImage getImage(){
        this.imageRequested = true;
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

    @Override
    public void close(){
        if(!this.imageRequested)
            this.image.close();
    }
}
