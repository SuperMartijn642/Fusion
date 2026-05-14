package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
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
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/color/item/ItemColors;getColor(Lnet/minecraft/world/item/ItemStack;I)I",
            shift = At.Shift.AFTER
        )
    )
    private void renderQuadList(CallbackInfo ci, @Local LocalRef<BakedQuad> quad, @Local(ordinal = 2) LocalIntRef color){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.get().tintIndex == 39216){
            TextureAtlasSprite sprite = quad.get().getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null)
                    color.set(QuadTintingHelper.getColor(tinting, null, null, null));
            }
        }
    }
}
