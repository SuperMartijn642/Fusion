package com.supermartijn642.fusion.model.types.base;

import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.model.quad.MutableQuad;
import com.supermartijn642.fusion.model.quad.MutableQuadImpl;
import com.supermartijn642.fusion.model.quad.QuadAccess;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private final QuadAccess quad;
    private final SpriteInstance spriteInstance;
    private final Direction cullDirection;

    public BaseModelQuad(BakedQuad bakedQuad, Direction cullDirection){
        this.spriteInstance = SpriteHelper.getSpriteInstance(bakedQuad.materialInfo().sprite());
        this.cullDirection = cullDirection;
        // Update quad properties from base texture data
        MutableQuad quad = new MutableQuadImpl();
        quad.copyBakedQuad(bakedQuad);
        if(this.spriteInstance != null && this.spriteInstance.getTexture().getCustomData() instanceof BaseTextureData data){
            if(data.getRenderType() != null){
                Transparency transparency = data.getRenderType() == BaseTextureData.RenderType.TRANSLUCENT ? Transparency.TRANSLUCENT :
                    data.getRenderType() == BaseTextureData.RenderType.CUTOUT ? Transparency.TRANSPARENT :
                        Transparency.NONE;
                quad.transparency(transparency);
            }
            if(data.isEmissive())
                quad.emissive(true);
            if(data.getTinting() != null)
                quad.tintIndex(39216);
        }
        this.quad = quad;
    }

    public QuadAccess quad(){
        return this.quad;
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
}
