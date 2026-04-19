package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @Inject(
        method = "getLayerColorSafe([ILnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void getLayerColorSafe(int[] tintLayers, BakedQuad.MaterialInfo material, CallbackInfoReturnable<Integer> ci){
        // Handle quads that have a custom Fusion tinting
        if(material.tintIndex() == 39216){
            // Get the sprite instance
            TextureAtlasSprite sprite = material.sprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                // Get custom tinting
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    // Apply tint
                    ci.setReturnValue(QuadTintingHelper.getDefaultColor(tinting, Blocks.AIR.defaultBlockState()));
                }
            }
        }
    }
}
