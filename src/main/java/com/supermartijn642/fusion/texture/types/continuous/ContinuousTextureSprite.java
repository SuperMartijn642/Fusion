package com.supermartijn642.fusion.texture.types.continuous;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.texture.data.ContinuousTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ContinuousTextureSprite extends BaseTextureSprite {

    protected ContinuousTextureSprite(TextureAtlasSprite original, ContinuousTextureData data){
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
    }

    @Override
    public ContinuousTextureData data(){
        return (ContinuousTextureData)super.data();
    }

    @Override
    public Transparency computeTransparency(float u0, float v0, float u1, float v1){
        ContinuousTextureData data = this.data();
        return computeTiledTransparency(this, u0, v0, u1, v1, data.getColumns(), data.getRows());
    }
}
