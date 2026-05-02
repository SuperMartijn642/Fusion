package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @Inject(method = "getLayerColorSafe([ILnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;)I", at = @At("HEAD"), cancellable = true)
    private static void getLayerColor(int[] tintLayers, BakedQuad.MaterialInfo material, CallbackInfoReturnable<Integer> ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(material.tintIndex() == 39216){
            TextureAtlasSprite sprite = material.sprite();
            if(sprite instanceof BaseTextureSprite){
                BaseTextureData.QuadTinting tinting = ((BaseTextureSprite)sprite).data().getTinting();
                if(tinting != null)
                    ci.setReturnValue(QuadTintingHelper.getColor(tinting, null, null, null));
            }
        }
    }
}
