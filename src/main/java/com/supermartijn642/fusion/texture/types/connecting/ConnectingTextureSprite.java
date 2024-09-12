package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends BaseTextureSprite {

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureData data){
        super(original, data);
    }

    @Override
    public ConnectingTextureData data(){
        return (ConnectingTextureData)super.data();
    }
}
