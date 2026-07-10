package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;"
        )
    )
    private BlockStateModel collectModelsByOffset(BlockStateModel model, @Local SubmitNodeStorage.MovingBlockSubmit submit, @Local BlockRenderDispatcher blockRenderer, @Local MultiBufferSource.BufferSource buffers){
        if(!(model instanceof BlockModelModifierBakedModel))
            return model;
        MovingBlockRenderState blockRenderState = submit.movingBlockRenderState();
        BlockState blockState = blockRenderState.blockState;
        BlockPos pos = blockRenderState.blockPos;
        BlockAndTintGetter level = blockRenderState.level;
        ModelDataManager modelDataManager = level.getModelDataManager();
        ModelData entityData = modelDataManager == null ? ModelData.EMPTY : modelDataManager.getAtOrEmpty(pos);
        // Create matrix stack and random source
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(submit.pose());
        RandomSource randomSource = new SingleThreadedRandomSource(0);
        long seed = blockState.getSeed(pos);
        // Submit models
        List<BlockModelPart> parts = new ArrayList<>();
        this.modelsByRandomOffset.setContext(pos, blockState.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, blockState);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    poseStack.pushPose();
                    ModelData modelData = entry.getModelData(level, pos, blockState, entityData);
                    randomSource.setSeed(seed);
                    entry.collectParts(randomSource, parts, modelData, null);
                    randomSource.setSeed(seed);
                    ChunkSectionLayer renderType = entry.getRenderTypes(blockState, randomSource, modelData).contains(ChunkSectionLayer.TRANSLUCENT) ?
                        ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;
                    blockRenderer.getModelRenderer().tesselateBlock(blockRenderState, parts, blockState, pos, poseStack, buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)), false, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                    parts.clear();
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        return blockRenderer.getBlockModel(Blocks.AIR.defaultBlockState());
    }
}
