package com.supermartijn642.fusion.texture.types.continuous;

import com.supermartijn642.fusion.api.texture.data.ContinuousTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ContinuousTextureSprite extends BaseTextureSprite {

    protected ContinuousTextureSprite(TextureAtlasSprite original, ContinuousTextureData data){
        super(original, data);
    }

    @Override
    public ContinuousTextureData data(){
        return (ContinuousTextureData)super.data();
    }
}
