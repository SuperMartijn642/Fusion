package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteConstructionContext;
import net.minecraft.resources.Identifier;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteConstructionContextImpl implements SpriteConstructionContext {

    private final int atlasWidth, atlasHeight;
    private final Identifier atlas;
    private final int mipmapLevels;
    private final int spritePadding;

    public SpriteConstructionContextImpl(int atlasWidth, int atlasHeight, Identifier atlas, int mipmapLevels, int spritePadding){
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.atlas = atlas;
        this.mipmapLevels = mipmapLevels;
        this.spritePadding = spritePadding;
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
    public int getSpritePadding(){
        return this.spritePadding;
    }

    @Override
    public int getMipmapLevels(){
        return this.mipmapLevels;
    }
}
