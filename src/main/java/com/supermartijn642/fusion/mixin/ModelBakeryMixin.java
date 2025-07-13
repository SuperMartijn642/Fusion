package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

    @ModifyReturnValue(
        method = "bakeModels",
        at = @At("RETURN")
    )
    private CompletableFuture<ModelBakery.BakingResult> applyBlockModelOverlays(CompletableFuture<ModelBakery.BakingResult> future, SpriteGetter textureGetter, Executor executor){
        return future.whenComplete((results, throwable) -> {
            //noinspection DataFlowIssue
            ModelBakery.ModelBakerImpl resolver = ((ModelBakery)(Object)this).new ModelBakerImpl(textureGetter);
            BlockModelModifierReloadListener.INSTANCE.applyOverlays(results, resolver);
            ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(results, new ItemModel.BakingContext(
                resolver,
                this.entityModelSet,
                results.missingModels().item(),
                null
            ));
        });
    }
}
