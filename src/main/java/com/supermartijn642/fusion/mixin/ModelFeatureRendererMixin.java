package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Inject(
        method = "renderBatch",
        at = @At("HEAD")
    )
    private void renderBatch(
        MultiBufferSource.BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        Map<RenderType,List<SubmitNodeStorage.ModelSubmit<?>>> map,
        MultiBufferSource.BufferSource bufferSource2,
        CallbackInfo ci
    ){
        FusionModelPart.BUFFER_SOURCE_CONTEXT.set(bufferSource);
    }

    @Inject(
        method = "renderBatch",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Iterator;next()Ljava/lang/Object;",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void renderBatchResetBuffer(
        MultiBufferSource.BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        Map<RenderType,List<SubmitNodeStorage.ModelSubmit<?>>> map,
        MultiBufferSource.BufferSource bufferSource2,
        CallbackInfo ci,
        @Local Map.Entry<RenderType,List<SubmitNodeStorage.ModelSubmit<?>>> entry,
        @Local LocalRef<VertexConsumer> vertexConsumer
    ){
        // Reset buffer in case a buffer for a different render type was requested when changing textures
        vertexConsumer.set(bufferSource.getBuffer(entry.getKey()));
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD")
    )
    private <S> void renderModel(
        SubmitNodeStorage.ModelSubmit<S> modelSubmit,
        RenderType renderType,
        VertexConsumer vertexConsumer,
        OutlineBufferSource outlineBufferSource,
        MultiBufferSource.BufferSource bufferSource,
        CallbackInfo ci
    ){
        if(modelSubmit.state() instanceof EntityRenderStateExtension extension)
            FusionModelPart.RENDER_STATE_CONTEXT.set(extension);
    }
}
