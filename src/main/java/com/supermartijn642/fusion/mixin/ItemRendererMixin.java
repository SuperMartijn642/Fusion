package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFIIZ)V"
        )
    )
    private void renderQuadList(VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, int lightmap, int overlay, boolean readExistingColor){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    red = (color >> 16 & 255) / 255f;
                    green = (color >> 8 & 255) / 255f;
                    blue = (color & 255) / 255f;
                }
            }
        }
        // Call the original method
        vertexConsumer.putBulkData(pose, quad, red, green, blue, lightmap, overlay, readExistingColor);
    }
}
