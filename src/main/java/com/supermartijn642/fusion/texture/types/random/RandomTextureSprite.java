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

    @Override
    public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn){
        super.initSprite(inX, inY, originInX, originInY, rotatedIn);
        this.resizeUV();
    }

    private void resizeUV(){
        this.maxU = this.minU + (this.maxU - this.minU) / this.data().getColumns();
        this.maxV = this.minV + (this.maxV - this.minV) / this.data().getRows();
    }
}
