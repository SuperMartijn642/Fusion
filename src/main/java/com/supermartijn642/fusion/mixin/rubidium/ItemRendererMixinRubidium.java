package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    return this.multARGBInts(quad.getColor(vertex), ColorARGB.toABGR(color));
                }
            }
        }
        // Call the original method
        return quad.getColor(vertex);
    }

    /**
     * Copied from Rubidium
     */
    @Unique
    private int multARGBInts(int colorA, int colorB){
        int a = (int)((float)ColorARGB.unpackAlpha(colorA) / 255.0F * ((float)ColorARGB.unpackAlpha(colorB) / 255.0F) * 255.0F);
        int b = (int)((float)ColorARGB.unpackBlue(colorA) / 255.0F * ((float)ColorARGB.unpackBlue(colorB) / 255.0F) * 255.0F);
        int g = (int)((float)ColorARGB.unpackGreen(colorA) / 255.0F * ((float)ColorARGB.unpackGreen(colorB) / 255.0F) * 255.0F);
        int r = (int)((float)ColorARGB.unpackRed(colorA) / 255.0F * ((float)ColorARGB.unpackRed(colorB) / 255.0F) * 255.0F);
        return ColorARGB.pack(r, g, b, a);
    }
}
