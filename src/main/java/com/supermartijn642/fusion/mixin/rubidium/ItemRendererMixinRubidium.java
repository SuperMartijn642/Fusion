package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(value = ItemRenderer.class, priority = 1001)
public class ItemRendererMixinRubidium {

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;getColor(I)I",
            remap = false
        )
    )
    private int renderQuadList(ModelQuadView quad, int vertex){
        // In case texture has a custom tinting set, replace the original tinting
        if(((BakedQuad)quad).getTintIndex() == 39216){
            TextureAtlasSprite sprite = ((BakedQuad)quad).getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    return this.multABGRInts(quad.getColor(vertex), ColorARGB.toABGR(color));
                }
            }
        }
        // Call the original method
        return quad.getColor(vertex);
    }

    /**
     * Copied from Rubidium
     */
    private int multABGRInts(int colorA, int colorB){
        if(colorA == -1){
            return colorB;
        }else if(colorB == -1){
            return colorA;
        }else{
            int a = (int)((float)ColorABGR.unpackAlpha(colorA) / 255.0F * ((float)ColorABGR.unpackAlpha(colorB) / 255.0F) * 255.0F);
            int b = (int)((float)ColorABGR.unpackBlue(colorA) / 255.0F * ((float)ColorABGR.unpackBlue(colorB) / 255.0F) * 255.0F);
            int g = (int)((float)ColorABGR.unpackGreen(colorA) / 255.0F * ((float)ColorABGR.unpackGreen(colorB) / 255.0F) * 255.0F);
            int r = (int)((float)ColorABGR.unpackRed(colorA) / 255.0F * ((float)ColorABGR.unpackRed(colorB) / 255.0F) * 255.0F);
            return ColorABGR.pack(r, g, b, a);
        }
    }
}
