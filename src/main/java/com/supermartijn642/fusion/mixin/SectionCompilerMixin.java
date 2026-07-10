package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Function;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @Final
    @Shadow
    private BlockRenderDispatcher blockRenderer;

    @ModifyExpressionValue(
        method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;"
        )
    )
    private BlockStateModel collectModelsByOffset(BlockStateModel model,
                                                  @Share("modelsByRandomOffset") LocalRef<ModelsByRandomOffset> sharedModels,
                                                  @Local BlockState blockState, @Local(ordinal = 2) BlockPos pos, @Local RenderChunkRegion renderRegion, @Local PoseStack poseStack, @Local Function<RenderType,VertexConsumer> buffers, @Local(ordinal = 1) List<BlockModelPart> parts, @Local RandomSource randomSource){
        if(!(model instanceof BlockModelModifierBakedModel))
            return model;
        ModelsByRandomOffset modelsByRandomOffset = sharedModels.get();
        if(modelsByRandomOffset == null)
            sharedModels.set(modelsByRandomOffset = new ModelsByRandomOffset());
        poseStack.pushPose();
        poseStack.translate(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
        long seed = blockState.getSeed(pos);
        modelsByRandomOffset.setContext(pos, blockState.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, renderRegion, pos, blockState);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    poseStack.pushPose();
                    randomSource.setSeed(seed);
                    entry.collectParts(renderRegion, pos, blockState, randomSource, parts);
                    this.blockRenderer.renderBatched(blockState, pos, renderRegion, poseStack, buffers, true, parts);
                    poseStack.popPose();
                    parts.clear();
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
            parts.clear();
            poseStack.popPose();
        }
        return this.blockRenderer.getBlockModel(Blocks.AIR.defaultBlockState());
    }
}
