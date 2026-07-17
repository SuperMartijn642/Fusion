package com.supermartijn642.fusion.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainLikeRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 10/07/2026 by SuperMartijn642
 */
@Mixin(TerrainLikeRenderContext.class)
public class TerrainLikeRenderContextMixin {

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private TerrainLikeRenderContextMixin(){
    }

    @Shadow
    private void bufferModel(BlockAndTintGetter level, BlockStateModel model, BlockState state, BlockPos pos, PoseStack poseStack, MultiBufferSource buffers, boolean cull, long seed, int overlay){
        throw new AssertionError();
    }

    @Inject(
        method = "bufferModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffset(BlockAndTintGetter level, BlockStateModel model, BlockState state, BlockPos pos, PoseStack poseStack, MultiBufferSource buffers, boolean cull, long seed, int overlay, CallbackInfo ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        this.modelsByRandomOffset.setContext(pos, state.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    poseStack.pushPose();
                    this.bufferModel(level, entry, state, pos, poseStack, buffers, cull, seed, overlay);
                    poseStack.popPose();
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.cancel();
    }

    @ModifyExpressionValue(
        method = "bufferModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original, @Local BlockStateModel model){
        if(model instanceof ModelsByRandomOffset.Entry entry){
            Vector3fc offset = entry.getOffset();
            return new Vec3(offset.x(), offset.y(), offset.z());
        }
        return original;
    }
}
