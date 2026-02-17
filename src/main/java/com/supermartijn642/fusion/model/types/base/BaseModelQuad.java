package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Loader;
import zone.rong.loliasm.bakedquad.SupportingBakedQuad;
import zone.rong.loliasm.config.LoliConfig;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private static final boolean isSquashBakedQuadEnabled = Loader.isModLoaded("loliasm") && LoliConfig.instance.squashBakedQuads;

    private final BakedQuad bakedQuad;
    private final TextureType<?> textureType;
    private final EnumFacing cullDirection;
    private final Integer lightEmission;
    private final BaseTextureData.RenderType renderType;
    private final boolean emissive;

    public BaseModelQuad(BakedQuad bakedQuad, EnumFacing cullDirection, Integer lightEmission){
        BakedQuad quad = bakedQuad;
        this.textureType = SpriteHelper.getTextureType(bakedQuad.getSprite());
        this.cullDirection = cullDirection;
        this.lightEmission = lightEmission;
        TextureAtlasSprite sprite = bakedQuad.getSprite();
        if(sprite instanceof BaseTextureSprite && ((BaseTextureSprite)sprite).data() != null){
            BaseTextureData data = ((BaseTextureSprite)sprite).data();
            this.renderType = data.getRenderType();
            this.emissive = data.isEmissive();
            if(data.getTinting() != null)
                if(isSquashBakedQuadEnabled) {
                    quad = new SupportingBakedQuad(quad.getVertexData(), 39216, quad.getFace(),
                            quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat());
                }else{
                    bakedQuad.tintIndex = 39216;
                }
        }else{
            this.renderType = null;
            this.emissive = false;
        }
        this.bakedQuad = quad;
    }

    public BakedQuad bakedQuad(){
        return this.bakedQuad;
    }

    public TextureType<?> textureType(){
        return this.textureType;
    }

    public EnumFacing cullDirection(){
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
