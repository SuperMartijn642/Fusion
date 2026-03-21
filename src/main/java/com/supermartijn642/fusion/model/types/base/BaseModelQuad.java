package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import zone.rong.loliasm.bakedquad.SupportingBakedQuad;
import zone.rong.loliasm.config.LoliConfig;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelQuad {

    private static final boolean isSquashBakedQuadEnabled = Loader.isModLoaded("loliasm") && LoliConfig.instance.squashBakedQuads;

    // This was being inlined causing class-loading, @Optional.Method annotation needed
    @Optional.Method(modid = "loliasm")
    private static BakedQuad newSupportingQuad(BakedQuad quad){
        return new SupportingBakedQuad(quad.getVertexData(), 39216, quad.getFace(),
            quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat());
    }

    private final BakedQuad bakedQuad;
    private final SpriteInstance spriteInstance;
    private final EnumFacing cullDirection;
    private final Integer lightEmission;
    private final BaseTextureData.RenderType renderType;
    private final boolean emissive;

    public BaseModelQuad(BakedQuad bakedQuad, EnumFacing cullDirection, Integer lightEmission){
        this.spriteInstance = SpriteHelper.getSpriteInstance(bakedQuad.getSprite());
        this.cullDirection = cullDirection;
        this.lightEmission = lightEmission;
        if(this.spriteInstance != null && this.spriteInstance.getTexture().getCustomData() instanceof BaseTextureData){
            BaseTextureData data = (BaseTextureData)this.spriteInstance.getTexture().getCustomData();
            this.renderType = data.getRenderType();
            this.emissive = data.isEmissive();
            if(data.getTinting() != null){
                if(isSquashBakedQuadEnabled)
                    bakedQuad = newSupportingQuad(bakedQuad);
                else
                    bakedQuad.tintIndex = 39216;
            }
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
