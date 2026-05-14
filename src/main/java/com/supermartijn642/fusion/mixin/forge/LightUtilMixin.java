package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 14/09/2024 by SuperMartijn642
 */
@Mixin(LightUtil.class)
public class LightUtilMixin {

    @Inject(
        method = "putBakedQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/model/pipeline/IVertexConsumer;setQuadTint(I)V",
            remap = false
        ),
        remap = false
    )
    private static void putBakedQuad(IVertexConsumer consumer, BakedQuad quad, CallbackInfo ci){
        // In case texture has a custom tinting set, mark the vertex consumer
        if(quad.tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null && consumer instanceof VertexLighterFlatExtension)
                    ((VertexLighterFlatExtension)consumer).setFusionCustomTinting(tinting);
            }
        }
    }
}
