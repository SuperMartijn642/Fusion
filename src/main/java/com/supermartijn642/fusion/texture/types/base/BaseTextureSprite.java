package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(ResourceLocation atlas, SpriteContents contents, int atlasWidth, int atlasHeight, int spriteX, int spriteY, BaseTextureData data){
        super(atlas, contents, atlasWidth, atlasHeight, spriteX, spriteY);
        this.data = data;
    }

    public BaseTextureData data(){
        return this.data;
    }
}
