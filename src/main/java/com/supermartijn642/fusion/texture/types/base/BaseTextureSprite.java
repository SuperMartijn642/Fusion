package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(TextureAtlasSprite original, BaseTextureData data){
        super(original.getIconName());
        this.copyFrom(original);
        this.framesTextureData = original.framesTextureData;
        this.animationMetadata = original.animationMetadata;
        this.data = data;
    }

    public BaseTextureData data(){
        return this.data;
    }
}
