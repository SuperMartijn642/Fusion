package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.supermartijn642.fusion.extensions.MaterialInfoExtension;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 01/05/2026 by SuperMartijn642
 */
@Mixin(BlockModelLighter.class)
public class BlockModelLighterMixin {

    @Inject(
        method = "prepareQuadAmbientOcclusion(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void checkQuadAOFlag(BlockAndTintGetter level, BlockState state, BlockPos centerPosition, BakedQuad quad, QuadInstance outputInstance, CallbackInfo ci){
        if(MaterialInfoExtension.getAmbientOcclusion(quad.materialInfo()))
            //noinspection DataFlowIssue
            ((BlockModelLighter)(Object)this).prepareQuadFlat(level, state, centerPosition, -1, quad, outputInstance);
    }
}
