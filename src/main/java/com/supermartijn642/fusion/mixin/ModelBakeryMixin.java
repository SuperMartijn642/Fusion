package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierReloadListener;
import com.supermartijn642.fusion.model.modifiers.item.ItemModelModifierReloadListener;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.SpriteGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
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
    @Final
    @Shadow
    private MaterialSet materials;
    @Final
    @Shadow
    private PlayerSkinRenderCache playerSkinRenderCache;

    @ModifyReturnValue(
        method = "bakeModels",
        at = @At("RETURN")
    )
    private CompletableFuture<ModelBakery.BakingResult> applyBlockModelOverlays(CompletableFuture<ModelBakery.BakingResult> future, SpriteGetter textureGetter, Executor executor){
        // Ignore non-vanilla model bakeries
        //noinspection ConstantValue,EqualsBetweenInconvertibleTypes
        if(!this.getClass().equals(ModelBakery.class))
            return future;

        return future.thenApply((results) -> {
            // Make sure model maps are mutable
            boolean blockModelsMutable = results.blockStateModels() instanceof HashMap;
            boolean itemModelsMutable = results.itemStackModels() instanceof HashMap;
            if(!blockModelsMutable || !itemModelsMutable){
                results = new ModelBakery.BakingResult(
                    results.missingModels(),
                    blockModelsMutable ? results.blockStateModels() : new HashMap<>(results.blockStateModels()),
                    itemModelsMutable ? results.itemStackModels() : new HashMap<>(results.itemStackModels()),
                    results.itemProperties()
                );
            }

            // Apply Fusion model modifiers
            ModelBakery.ModelBakerImpl resolver = ((ModelBakery)(Object)this).new ModelBakerImpl(textureGetter, new ModelBakery.PartCacheImpl(), results.missingModels());
            BlockModelModifierReloadListener.INSTANCE.applyOverlays(results, resolver);
            ItemModelModifierReloadListener.INSTANCE.applyPredicateModels(results, new ItemModel.BakingContext(
                resolver,
                this.entityModelSet,
                this.materials,
                this.playerSkinRenderCache,
                results.missingModels().item(),
                null
            ));
            return results;
        });
    }
}
