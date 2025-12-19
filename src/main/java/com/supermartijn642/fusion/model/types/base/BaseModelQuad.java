package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private final BakedQuad bakedQuad;
    private final TextureType<?> textureType;
    private final Direction cullDirection;
    private final BaseTextureData.RenderType renderType;
    private final boolean emissive;

    public BaseModelQuad(BakedQuad bakedQuad, Direction cullDirection){
        this.textureType = SpriteHelper.getTextureType(bakedQuad.sprite());
        this.cullDirection = cullDirection;
        TextureAtlasSprite sprite = bakedQuad.sprite();
        if(sprite instanceof BaseTextureSprite && ((BaseTextureSprite)sprite).data() != null){
            BaseTextureData data = ((BaseTextureSprite)sprite).data();
            this.renderType = data.getRenderType();
            this.emissive = data.isEmissive();
            if(data.getTinting() != null) // Create an identical quad, but with tint index '39216'
                bakedQuad = new BakedQuad(bakedQuad.position0(), bakedQuad.position1(), bakedQuad.position2(), bakedQuad.position3(), bakedQuad.packedUV0(), bakedQuad.packedUV1(), bakedQuad.packedUV2(), bakedQuad.packedUV3(), 39216, bakedQuad.direction(), bakedQuad.sprite(), bakedQuad.shade(), bakedQuad.lightEmission(), bakedQuad.ambientOcclusion());
        }else{
            this.renderType = null;
            this.emissive = false;
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

    public BaseTextureData.RenderType renderType(){
        return this.renderType;
    }

    public boolean emissive(){
        return this.emissive;
    }
}
