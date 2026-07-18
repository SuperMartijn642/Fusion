package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Created 22/08/2025 by SuperMartijn642
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Final
    @Shadow
    private ModelBlockRenderer modelRenderer;
    @Final
    @Shadow
    private RandomSource singleThreadRandom;
    @Final
    @Shadow
    private List<BlockModelPart> singleThreadPartList;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private BlockRenderDispatcherMixin(){
    }

    @Inject(
        method = "renderBreakingTexture",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void collectModelsByOffset(BlockState blockState, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer buffer, CallbackInfo ci, @Local BlockStateModel model) {
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        long seed = blockState.getSeed(pos);
        this.modelsByRandomOffset.setContext(pos, blockState.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, blockState);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    poseStack.pushPose();
                    this.singleThreadRandom.setSeed(seed);
                    this.singleThreadPartList.clear();
                    entry.collectParts(level, pos, blockState, this.singleThreadRandom, this.singleThreadPartList);
                    this.modelRenderer.tesselateBlock(level, this.singleThreadPartList, blockState, pos, poseStack, type -> buffer, true, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        ci.cancel();
    }

    @Inject(
        method = "renderBreakingTexture",
        at = @At("HEAD")
    )
    private void renderBreakingTextureHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
    }

    @Inject(
        method = "renderBreakingTexture",
        at = @At("TAIL")
    )
    private void renderBreakingTextureTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }
}
