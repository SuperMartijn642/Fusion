package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.model.pipeline.LightUtil;
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
            target = "Lnet/minecraftforge/client/model/pipeline/LightUtil;renderQuadColor(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/model/BakedQuad;I)V"
        )
    )
    private void renderQuadList(BufferBuilder vertexConsumer, BakedQuad quad, int color){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    color = QuadTintingHelper.getColor(tinting, null, null, null);
                    color = color | -16777216;
                }
            }
        }
        // Call the original method
        LightUtil.renderQuadColor(vertexConsumer, quad, color);
    }
}
