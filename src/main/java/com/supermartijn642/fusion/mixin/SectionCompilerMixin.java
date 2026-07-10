package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @Final
    @Shadow
    private BlockRenderDispatcher blockRenderer;

    @Shadow
    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer,BufferBuilder> bufferStore, SectionBufferBuilderPack bufferCreator, ChunkSectionLayer renderType){
        throw new AssertionError();
    }

    @ModifyExpressionValue(
        method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;"
        )
    )
    private BlockStateModel collectModelsByOffset(BlockStateModel model,
                                                  @Share("modelsByRandomOffset") LocalRef<ModelsByRandomOffset> sharedModels,
                                                  @Local BlockState blockState, @Local(ordinal = 2) BlockPos pos, @Local RenderSectionRegion renderRegion, @Local PoseStack poseStack, @Local List<BlockModelPart> parts, @Local RandomSource randomSource, @Local(ordinal = 0) Map<BlockPos,ModelData> modelDataMap, @Local(ordinal = 1) Map<ChunkSectionLayer,BufferBuilder> bufferStore, @Local SectionBufferBuilderPack bufferCreator){
        if(!(model instanceof BlockModelModifierBakedModel))
            return model;
        ModelsByRandomOffset modelsByRandomOffset = sharedModels.get();
        if(modelsByRandomOffset == null)
            sharedModels.set(modelsByRandomOffset = new ModelsByRandomOffset());
        poseStack.pushPose();
        poseStack.translate(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
        ModelData entityData = modelDataMap.get(pos);
        long seed = blockState.getSeed(pos);
        modelsByRandomOffset.setContext(pos, blockState.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, renderRegion, pos, blockState);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    poseStack.pushPose();
                    ModelData modelData = entry.getModelData(renderRegion, pos, blockState, entityData);
                    randomSource.setSeed(seed);
                    for(ChunkSectionLayer renderType : entry.getRenderTypes(blockState, randomSource, modelData)){
                        BufferBuilder buffer = this.getOrBeginLayer(bufferStore, bufferCreator, renderType);
                        randomSource.setSeed(seed);
                        entry.collectParts(randomSource, parts, modelData, renderType);
                        this.blockRenderer.renderBatched(blockState, pos, renderRegion, poseStack, buffer, true, parts);
                        parts.clear();
                    }
                    poseStack.popPose();
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
