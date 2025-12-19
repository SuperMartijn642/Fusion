package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteCreationContextImpl implements SpriteCreationContext, AutoCloseable {

    private final TextureAtlasSprite original;
    private final int textureWidth, textureHeight;
    private final Identifier identifier;
    private final NativeImage[] images;
    private final int atlasWidth, atlasHeight;
    private final Identifier atlas;
    private final int spriteX, spriteY, spriteWidth, spriteHeight, spritePadding;
    private final int mipmapLevels;
    private boolean imagesRequested = false;

    @SuppressWarnings("resource")
    public SpriteCreationContextImpl(SpriteLoader.Preparations preparations, TextureAtlasSprite original){
        this.original = original;
        this.textureWidth = original.contents().originalImage.getWidth();
        this.textureHeight = original.contents().originalImage.getHeight();
        this.identifier = original.contents().name();
        this.images = original.contents().byMipLevel;
        this.atlasWidth = preparations.width();
        this.atlasHeight = preparations.height();
        this.atlas = original.atlasLocation();
        this.spriteX = original.getX();
        this.spriteY = original.getY();
        this.spriteWidth = original.contents().width();
        this.spriteHeight = original.contents().height();
        this.spritePadding = original.padding;
        this.mipmapLevels = preparations.mipLevel();
    }

    private void closeUnusedResources(){
        if(!this.imagesRequested)
            this.original.contents().close();
    }

    @Override
    public TextureAtlasSprite createOriginalSprite(){
        this.imagesRequested = true;
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
    public Identifier getTextureIdentifier(){
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
    public Identifier getAtlasLocation(){
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
    public int getSpritePadding(){
        return this.spritePadding;
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
