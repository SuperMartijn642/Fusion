package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteConstructionContext;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class SpriteConstructionContextImpl implements SpriteConstructionContext {

    private final int atlasWidth, atlasHeight;
    private final ResourceLocation atlas;
    private final int mipmapLevels;

    public SpriteConstructionContextImpl(int atlasWidth, int atlasHeight, ResourceLocation atlas, int mipmapLevels){
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
    public ResourceLocation getAtlasLocation(){
        return this.atlas;
    }

    @Override
    public int getMipmapLevels(){
        return this.mipmapLevels;
    }
}
