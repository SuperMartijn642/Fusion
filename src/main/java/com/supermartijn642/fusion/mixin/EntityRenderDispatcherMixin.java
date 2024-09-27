package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.extensions.EntityRendererExtension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Created 30/09/2024 by SuperMartijn642
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void renderHead(Entity entity, double relativeEntityX, double relativeEntityY, double relativeEntityZ, float entityRotation, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int lighting, CallbackInfo ci, EntityRenderer<?> renderer){
        if(((EntityRendererExtension)renderer).getFusionModelParts() != null){
            for(FusionModelPart part : ((EntityRendererExtension)renderer).getFusionModelParts())
                part.setup(entity, bufferSource);
        }
    }

    @Inject(
        method = "render",
        at = @At("RETURN"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void renderTail(Entity entity, double relativeEntityX, double relativeEntityY, double relativeEntityZ, float entityRotation, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int lighting, CallbackInfo ci, EntityRenderer<?> renderer){
        if(((EntityRendererExtension)renderer).getFusionModelParts() != null){
            for(FusionModelPart part : ((EntityRendererExtension)renderer).getFusionModelParts())
                part.clear();
        }
    }
}
