package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.SpriteCreationContext;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteCreationContextImpl implements SpriteCreationContext, AutoCloseable {

    private final TextureAtlasSprite original;
    private final int textureWidth, textureHeight;
    private final ResourceLocation identifier;
    private final List<int[][]> images;
    private final int atlasWidth, atlasHeight;
    private final TextureMap atlas;
    private final int spriteX, spriteY, spriteWidth, spriteHeight;
    private final int mipmapLevels;
    private boolean imagesRequested = false;

    public SpriteCreationContextImpl(TextureAtlasSprite original, TextureMap atlas){
        this.original = original;
        Pair<Integer,Integer> textureSize = ((TextureAtlasSpriteExtension)original).getTextureSize();
        this.textureWidth = textureSize.left();
        this.textureHeight = textureSize.right();
        this.identifier = new ResourceLocation(original.getIconName());
        this.images = original.framesTextureData;
        this.atlasWidth = Math.round((original.originX + original.width) / original.maxU);
        this.atlasHeight = Math.round((original.originY + original.height) / original.maxV);
        this.atlas = atlas;
        this.spriteX = original.originX;
        this.spriteY = original.originY;
        this.spriteWidth = original.width;
        this.spriteHeight = original.height;
        this.mipmapLevels = original.framesTextureData.size() - 1;
    }

    private void closeUnusedResources(){
        if(!this.imagesRequested)
            this.original.clearFramesTextureData();
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
    public ResourceLocation getTextureIdentifier(){
        return this.identifier;
    }

    @Override
    public List<int[][]> getTextureBuffers(){
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
    public TextureMap getAtlas(){
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
