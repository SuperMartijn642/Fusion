package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private final BakedQuad bakedQuad;
    private final SpriteInstance spriteInstance;
    private final Direction cullDirection;
    private final BaseTextureData.RenderType renderType;
    private final boolean emissive;

    public BaseModelQuad(BakedQuad bakedQuad, Direction cullDirection){
        this.spriteInstance = SpriteHelper.getSpriteInstance(bakedQuad.sprite());
        this.cullDirection = cullDirection;
        if(this.spriteInstance != null && this.spriteInstance.getTexture().getCustomData() instanceof BaseTextureData data){
            this.renderType = data.getRenderType();
            this.emissive = data.isEmissive();
            if(data.getTinting() != null)
                bakedQuad = new BakedQuad(bakedQuad.vertices(), 39216, bakedQuad.direction(), bakedQuad.sprite(), bakedQuad.shade(), bakedQuad.lightEmission(), bakedQuad.ambientOcclusion());
        }else{
            this.renderType = null;
            this.emissive = false;
        }
        this.bakedQuad = bakedQuad;
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

    public BaseTextureData.RenderType renderType(){
        return this.renderType;
    }

    public boolean emissive(){
        return this.emissive;
    }
}
