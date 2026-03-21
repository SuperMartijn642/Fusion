package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteConstructionContext;
import net.minecraft.client.renderer.texture.AtlasTexture;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteConstructionContextImpl implements SpriteConstructionContext {

    private final int atlasWidth, atlasHeight;
    private final AtlasTexture atlas;
    private final int mipmapLevels;

    public SpriteConstructionContextImpl(int atlasWidth, int atlasHeight, AtlasTexture atlas, int mipmapLevels){
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.atlas = atlas;
        this.mipmapLevels = mipmapLevels;
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
    public AtlasTexture getTextureAtlas(){
        return this.atlas;
    }

    @Override
    public int getMipmapLevels(){
        return this.mipmapLevels;
    }
}
