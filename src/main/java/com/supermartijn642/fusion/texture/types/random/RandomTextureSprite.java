package com.supermartijn642.fusion.texture.types.random;

import com.supermartijn642.fusion.api.texture.data.RandomTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class RandomTextureSprite extends BaseTextureSprite {

    protected RandomTextureSprite(TextureAtlasSprite original, RandomTextureData data){
        super(original, data);
    }

    @Override
    public RandomTextureData data(){
        return (RandomTextureData)super.data();
    }
}
