package com.supermartijn642.fusion.texture.types.biome;

import com.supermartijn642.fusion.api.texture.data.BiomeTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class BiomeTextureSprite extends BaseTextureSprite {

    protected BiomeTextureSprite(TextureAtlasSprite original, BiomeTextureData data){
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
    public BiomeTextureData data(){
        return (BiomeTextureData)super.data();
    }
}
