package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.SpriteGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Created 28/12/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Final
    @Shadow
    private EntityModelSet entityModelSet;

    @Inject(
        method = "bakeModels",
        at = @At("RETURN"),
        cancellable = true
    )
    private void applyBlockModelOverlays(SpriteGetter textureGetter, Executor executor, CallbackInfoReturnable<CompletableFuture<ModelBakery.BakingResult>> ci){
        ci.setReturnValue(ci.getReturnValue().whenComplete((results, throwable) -> {
            //noinspection DataFlowIssue
            ModelBakery.ModelBakerImpl resolver = ((ModelBakery)(Object)this).new ModelBakerImpl(textureGetter);
            BlockModelModifierReloadListener.INSTANCE.applyOverlays(results, resolver);
            ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(results, new ItemModel.BakingContext(
                resolver,
                this.entityModelSet,
                results.missingModels().item(),
                null
            ));
        }));
    }
}
