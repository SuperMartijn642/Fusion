package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.BlockBreakingStateExtension;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Created 16/06/2026 by SuperMartijn642
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Final
    @Shadow
    private ModelManager modelManager;

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/block/BlockStateModelSet;get(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;",
            shift = At.Shift.AFTER
        )
    )
    private void submitBlockDestroyAnimation(PoseStack poseStack, SubmitNodeCollector output, LevelRenderState levelState, CallbackInfo ci, @Local BlockBreakingRenderState breakingState, @Local LocalRef<BlockStateModel> model){
        //noinspection DataFlowIssue
        List<Pair<Vector3fc,List<BlockStateModelPart>>> parts = ((BlockBreakingStateExtension)(Object)breakingState).getFusionParts();
        if(parts == null)
            return;
        // Undo normal offset
        Vec3 normalOffset = breakingState.blockState().getOffset(breakingState.blockPos());
        poseStack.translate(-normalOffset.x(), -normalOffset.y(), -normalOffset.z());
        // Submit models
        for(Pair<Vector3fc,List<BlockStateModelPart>> entry : parts){
            poseStack.pushPose();
            Vector3fc offset = entry.left();
            poseStack.translate(offset.x(), offset.y(), offset.z());
            output.submitBreakingBlockModel(poseStack, entry.right(), breakingState.progress());
            poseStack.popPose();
        }
        model.set(this.modelManager.getBlockStateModelSet().get(Blocks.AIR.defaultBlockState()));
    }

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At("HEAD")
    )
    private void submitBlockDestroyAnimationHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
    }

    @Inject(
        method = "submitBlockDestroyAnimation",
        at = @At("HEAD")
    )
    private void submitBlockDestroyAnimationTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }
}
