package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Inject(
        method = "prepareModel",
        at = @At("HEAD")
    )
    private <S> void prepareModelHead(ModelFeatureRenderer.Submit<S> submit, CallbackInfo ci){
        if(submit.state() instanceof EntityRenderStateExtension extension && extension.hasFusionContext()){
            //noinspection DataFlowIssue
            RenderTypeFeatureRenderer<?> featureRenderer = (RenderTypeFeatureRenderer<?>)(Object)this;
            FusionModelPart.RENDER_CONTEXT.set(new FusionModelPart.RenderContext(
                extension, submit.renderType(), featureRenderer::getVertexBuilder
            ));
        }
    }

    @Inject(
        method = "prepareModel",
        at = @At("TAIL")
    )
    private <S> void prepareModelTail(ModelFeatureRenderer.Submit<S> submit, CallbackInfo ci){
        if(submit.state() instanceof EntityRenderStateExtension extension && extension.hasFusionContext())
            FusionModelPart.RENDER_CONTEXT.remove();
    }
}
