package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends TextureAtlasSprite {

    private final ConnectingTextureLayout layout;
    private final ConnectingTextureData.RenderType renderType;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureLayout layout, ConnectingTextureData.RenderType renderType){
        super(original.getIconName());
        this.layout = layout;
        this.renderType = renderType;
        this.copyFrom(original);
        this.framesTextureData = original.framesTextureData;
        this.animationMetadata = original.animationMetadata;
        this.resizeUV();
    }

    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }

    public ConnectingTextureData.RenderType getRenderType(){
        return this.renderType;
    }

    @Override
    public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn){
        super.initSprite(inX, inY, originInX, originInY, rotatedIn);
        this.resizeUV();
    }

    private void resizeUV(){
        this.maxU = this.minU + (this.maxU - this.minU) / ConnectingTextureLayoutHelper.getWidth(this.layout);
        this.maxV = this.minV + (this.maxV - this.minV) / ConnectingTextureLayoutHelper.getHeight(this.layout);
    }
}
