package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteCreationContextImpl implements SpriteCreationContext, AutoCloseable {

    private final TextureAtlasSprite original;
    private final int textureWidth, textureHeight;
    private final ResourceLocation identifier;
    private final NativeImage[] images;
    private final int atlasWidth, atlasHeight;
    private final TextureAtlas atlas;
    private final int spriteX, spriteY, spriteWidth, spriteHeight;
    private final int mipmapLevels;
    private boolean originalRequested = false;
    private boolean imagesRequested = false;

    public SpriteCreationContextImpl(TextureAtlasSprite original){
        this.original = original;
        this.textureWidth = original.mainImage[0].getWidth();
        this.textureHeight = original.mainImage[0].getHeight();
        this.identifier = original.getName();
        this.images = original.mainImage;
        this.atlasWidth = Math.round((original.getX() + original.getWidth()) / original.getU1());
        this.atlasHeight = Math.round((original.getY() + original.getHeight()) / original.getV1());
        this.atlas = original.atlas();
        this.spriteX = original.getX();
        this.spriteY = original.getY();
        this.spriteWidth = original.getWidth();
        this.spriteHeight = original.getHeight();
        this.mipmapLevels = original.mainImage.length - 1;
    }

    private void closeUnusedResources(){
        if(!this.originalRequested){
            if(!this.imagesRequested)
                this.original.close();
            else if(this.original.animatedTexture != null)
                this.original.animatedTexture.close();
        }
    }

    @Override
    public TextureAtlasSprite createOriginalSprite(){
        this.originalRequested = true;
        return this.original;
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
    public ResourceLocation getTextureIdentifier(){
        return this.identifier;
    }

    @Override
    public NativeImage[] getTextureBuffers(){
        this.imagesRequested = true;
        return this.images;
    }

    @Override
    public int getAtlasWidth(){
        return this.atlasWidth;
    }

    @Override
    public int getAtlasHeight(){
        return this.atlasHeight;
    }

    @Override
    public TextureAtlas getAtlas(){
        return this.atlas;
    }

    @Override
    public int getSpritePositionX(){
        return this.spriteX;
    }

    @Override
    public int getSpritePositionY(){
        return this.spriteY;
    }

    @Override
    public int getSpriteWidth(){
        return this.spriteWidth;
    }

    @Override
    public int getSpriteHeight(){
        return this.spriteHeight;
    }

    @Override
    public int getMipmapLevels(){
        return this.mipmapLevels;
    }

    @Override
    public void close(){
        this.closeUnusedResources();
    }
}
