package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.SpritePreparationContext;
import net.minecraft.util.ResourceLocation;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class SpritePreparationContextImpl implements SpritePreparationContext {

    private final int originalWidth, originalHeight;
    private final int textureWidth, textureHeight;
    private final ResourceLocation identifier;

    public SpritePreparationContextImpl(int originalWidth, int originalHeight, int textureWidth, int textureHeight, ResourceLocation identifier){
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.identifier = identifier;
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
}
