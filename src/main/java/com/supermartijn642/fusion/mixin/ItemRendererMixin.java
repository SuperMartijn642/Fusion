package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
        method = "renderModelLists",
        at = @At("HEAD")
    )
    private void captureItemStackContext(IBakedModel model, ItemStack stack, int lighting, int overlay, MatrixStack matrixStack, IVertexBuilder vertexConsumer, CallbackInfo ci) {
        FusionClient.ITEM_STACK_RENDER_CONTEXT.set(stack);
    }

    @Inject(
        method = "renderModelLists",
        at = @At("TAIL")
    )
    private void releaseItemStackContext(CallbackInfo ci) {
        FusionClient.ITEM_STACK_RENDER_CONTEXT.remove();
    }

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/IVertexBuilder;addVertexData(Lcom/mojang/blaze3d/matrix/MatrixStack$Entry;Lnet/minecraft/client/renderer/model/BakedQuad;FFFIIZ)V"
        )
    )
    private void renderQuadList(IVertexBuilder vertexConsumer, MatrixStack.Entry pose, BakedQuad quad, float red, float green, float blue, int lightmap, int overlay, boolean readExistingColor){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    red = (color >> 16 & 255) / 255f;
                    green = (color >> 8 & 255) / 255f;
                    blue = (color & 255) / 255f;
                }
            }
        }
        // Call the original method
        vertexConsumer.addVertexData(pose, quad, red, green, blue, lightmap, overlay, readExistingColor);
    }
}
