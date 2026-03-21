package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private final BakedQuad bakedQuad;
    private final SpriteInstance spriteInstance;
    private final Direction cullDirection;
    private final Integer lightEmission;
    private final BaseTextureData.RenderType renderType;
    private final boolean emissive;

    public BaseModelQuad(BakedQuad bakedQuad, Direction cullDirection, Integer lightEmission){
        this.bakedQuad = bakedQuad;
        this.spriteInstance = SpriteHelper.getSpriteInstance(bakedQuad.getSprite());
        this.cullDirection = cullDirection;
        this.lightEmission = lightEmission;
        if(this.spriteInstance != null && this.spriteInstance.getTexture().getCustomData() instanceof BaseTextureData){
            BaseTextureData data = (BaseTextureData)this.spriteInstance.getTexture().getCustomData();
            this.renderType = data.getRenderType();
            this.emissive = data.isEmissive();
            if(data.getTinting() != null)
                bakedQuad.tintIndex = 39216;
        }else{
            this.renderType = null;
            this.emissive = false;
        }
    }

    public BakedQuad bakedQuad(){
        return this.bakedQuad;
    }

    public SpriteInstance spriteInstance(){
        return this.spriteInstance;
    }

    public TextureType<?,?> textureType(){
        return this.spriteInstance == null ? DefaultTextureTypes.VANILLA : this.spriteInstance.getTexture().getTextureType();
    }

    public Direction cullDirection(){
        return this.cullDirection;
    }

    public Integer lightEmission(){
        return this.lightEmission;
    }

    public BaseTextureData.RenderType renderType(){
        return this.renderType;
    }

    public boolean emissive(){
        return this.emissive;
    }
}
