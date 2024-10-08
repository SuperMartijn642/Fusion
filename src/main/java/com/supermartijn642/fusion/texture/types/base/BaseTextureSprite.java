package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseTextureSprite extends TextureAtlasSprite {

    private final BaseTextureData data;

    public BaseTextureSprite(TextureAtlasSprite original, BaseTextureData data){
        super(original.getName(), original.width, original.height);
        this.mainImage = original.mainImage;
        this.metadata = original.metadata;
        this.framesX = original.framesX;
        this.framesY = original.framesY;
        this.x = original.x;
        this.y = original.y;
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
        this.data = data;
    }

    public BaseTextureData data(){
        return this.data;
    }
}
