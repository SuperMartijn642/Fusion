package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends BaseTextureSprite {

    private final float startU, startV;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureData data, float startU, float startV){
        super(
            original.atlasLocation(),
            original.contents(),
            1,
            1,
            original.getX(),
            original.getY(),
            data
        );
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
        this.startU = startU;
        this.startV = startV;
    }

    public float getStartU(){
        return this.startU;
    }

    public float getStartV(){
        return this.startV;
    }

    @Override
    public ConnectingTextureData data(){
        return (ConnectingTextureData)super.data();
    }
}
