package com.supermartijn642.fusion.mixin.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;

/**
 * Created 20/12/2025 by SuperMartijn642
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixinSodium {

    /*
        The goal is to overwrite the quad tinting that normally occurs in ItemRenderer#renderQuadList.
        Sodium intercepts #renderQuadList in ItemRenderer#renderItem with a mixin and applies the quad
        tinting themselves in their mixin.

        Fusion needs the sprite and hence the quad as context for the tinting.

        Sodium calls #getLayerColorSafe once per quad, so we capture an iterator for the list of quads
        in #renderItem and then iterate through them whenever #getLayerColorSafe is called.
        Then from #getLayerColorSafe we return Fusion's tinting.
     */

    @Unique
    private static final ThreadLocal<Iterator<BakedQuad>> lastSubmittedQuads = new ThreadLocal<>();

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;[III)V",
            shift = At.Shift.BEFORE
        )
    )
    private static void captureQuads(ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, int[] colors, List<BakedQuad> quads, RenderType renderType, ItemStackRenderState.FoilType foilType, CallbackInfo ci){
        for(BakedQuad quad : quads){
            //noinspection resource
            if(quad.tintIndex() == 39216 && quad.sprite() instanceof BaseTextureSprite){
                lastSubmittedQuads.set(quads.iterator());
                break;
            }
        }
    }

    @Inject(
        method = "getLayerColorSafe",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void overwriteTinting(int[] colors, int tintIndex, CallbackInfoReturnable<Integer> ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(tintIndex != 39216)
            return;

        // Find the relevant quad
        Iterator<BakedQuad> quads = lastSubmittedQuads.get();
        if(quads == null)
            return;
        BaseTextureSprite sprite = null;
        while(quads.hasNext()){
            BakedQuad quad = quads.next();
            //noinspection resource
            if(quad.tintIndex() == 39216 && quad.sprite() instanceof BaseTextureSprite s){
                sprite = s;
                break;
            }
        }
        if(sprite == null)
            return;

        // Calculate tinting
        BaseTextureData.QuadTinting tinting = sprite.data().getTinting();
        if(tinting != null)
            ci.setReturnValue(QuadTintingHelper.getColor(tinting, null, null, null));
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;[III)V",
            shift = At.Shift.AFTER
        )
    )
    private static void clearQuads(CallbackInfo ci){
        lastSubmittedQuads.remove();
    }
}
