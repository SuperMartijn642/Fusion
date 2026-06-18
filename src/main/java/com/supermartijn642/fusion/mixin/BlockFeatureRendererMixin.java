package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 15/04/2026 by SuperMartijn642
 */
@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {

    @Inject(
        method = "putQuad",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V",
            shift = At.Shift.AFTER
        )
    )
    private static void putQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance, int[] tintLayers, VertexConsumer buffer, VertexConsumer outlineBuffer, CallbackInfo ci){
        // Handle quads that have a custom Fusion tinting
        if(quad.materialInfo().tintIndex() == 39216){
            // Get the sprite instance
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                // Get custom tinting
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    // Apply tint
                    instance.setColor(QuadTintingHelper.getDefaultColor(tinting, Blocks.AIR.defaultBlockState()));
                }
            }
        }
    }

    @Inject(
        method = "renderBreakingBlockModelSubmits",
        at = @At("HEAD")
    )
    private void renderBreakingBlockModelSubmitsHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
    }

    @Inject(
        method = "renderBreakingBlockModelSubmits",
        at = @At("TAIL")
    )
    private void renderBreakingBlockModelSubmitsTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }
}
