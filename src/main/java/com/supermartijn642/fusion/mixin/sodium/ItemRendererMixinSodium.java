package com.supermartijn642.fusion.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(value = ItemRenderer.class, priority = 1001)
public class ItemRendererMixinSodium {

    @Inject(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lme/jellysquid/mods/sodium/client/util/color/ColorARGB;toABGR(II)I",
            shift = At.Shift.AFTER,
            by = 2,
            remap = false
        )
    )
    private void renderQuadList(CallbackInfo ci, @Local(ordinal = 2) LocalIntRef color, @Local BakedQuad quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null)
                    color.set(ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, null, null, null), 255));
            }
        }
    }
}
