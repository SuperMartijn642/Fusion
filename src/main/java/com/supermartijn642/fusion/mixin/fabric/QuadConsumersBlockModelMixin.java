package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.renderer.QuadConsumers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 15/04/2026 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(QuadConsumers.BlockModel.class)
public class QuadConsumersBlockModelMixin {

    @Inject(
        method = "accept",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;buffer(ILcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void accept(MutableQuadView quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(quad);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int tint = QuadTintingHelper.getDefaultColor(tinting, Blocks.AIR.defaultBlockState());
                    quad.multiplyColor(tint);
                }
            }
        }
    }
}
