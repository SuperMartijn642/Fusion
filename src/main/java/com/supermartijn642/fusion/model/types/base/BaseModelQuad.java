package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private final BakedQuad bakedQuad;
    private final TextureType<?> textureType;
    private final Direction cullDirection;
    private final boolean emissive;
    private final int tintIndex;

    public BaseModelQuad(BakedQuad bakedQuad, Direction cullDirection){
        BakedQuad.MaterialInfo materialInfo = bakedQuad.materialInfo();
        TextureAtlasSprite sprite = materialInfo.sprite();
        this.textureType = SpriteHelper.getTextureType(sprite);
        this.cullDirection = cullDirection;
        if(sprite instanceof BaseTextureSprite && ((BaseTextureSprite)sprite).data() != null){
            BaseTextureData data = ((BaseTextureSprite)sprite).data();
            this.emissive = data.isEmissive();
            this.tintIndex = data.getTinting() != null ? 39216 : bakedQuad.materialInfo().tintIndex();
        }else{
            this.emissive = false;
            this.tintIndex = bakedQuad.materialInfo().tintIndex();
        }
        this.bakedQuad = bakedQuad;
    }

    public BakedQuad bakedQuad(){
        return this.bakedQuad;
    }

    public TextureType<?> textureType(){
        return this.textureType;
    }

    public Direction cullDirection(){
        return this.cullDirection;
    }

    public boolean emissive(){
        return this.emissive;
    }

    public void fill(MutableQuad mutableQuad){
        mutableQuad.fillFromBakedQuad(this.bakedQuad);
        mutableQuad.emissive(this.emissive);
        mutableQuad.tintIndex(this.tintIndex);
    }
}
