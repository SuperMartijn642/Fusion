package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.extensions.ModelExtension;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 18/09/2024 by SuperMartijn642
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Shadow
    private EntityModel<?> model;

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            shift = At.Shift.BEFORE
        )
    )
    private void flipModel(LivingEntity entity, float rotation, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int lighting, CallbackInfo ci){
        if(this.model instanceof ModelExtension && ((ModelExtension)this.model).containsFusionModel()){
            poseStack.pushPose();
            poseStack.translate(0, 1.501f, 0);
            poseStack.scale(-1, -1, 1);
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            shift = At.Shift.AFTER
        )
    )
    private void unflipModel(LivingEntity entity, float rotation, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int lighting, CallbackInfo ci){
        if(this.model instanceof ModelExtension && ((ModelExtension)this.model).containsFusionModel())
            poseStack.popPose();
    }
}
