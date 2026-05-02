package com.supermartijn642.fusion.texture.types.connecting;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
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
            original.padding,
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

    @Override
    public Transparency computeTransparency(float u0, float v0, float u1, float v1){
        ConnectingTextureLayoutHandler handler = ConnectingTextureLayoutHandler.get(this.data().getLayout());
        return computeTiledTransparency(this, u0, v0, u1, v1, handler.getWidth(), handler.getHeight());
    }
}
