package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Created 22/08/2025 by SuperMartijn642
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Final
    @Shadow
    private BlockModelShaper blockModelShaper;
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
        method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;",
            shift = At.Shift.AFTER
        )
    )
    private void collectModelsByOffset(BlockState blockState, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer buffer, ModelData entityData, CallbackInfo ci, @Local BlockStateModel model) {
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
                    this.singleThreadPartList.clear();
                    this.singleThreadRandom.setSeed(seed);
                    ModelData modelData = entry.getModelData(level, pos, blockState, entityData);
                    this.singleThreadRandom.setSeed(seed);
                    entry.collectParts(this.singleThreadRandom, this.singleThreadPartList, modelData, null);
                    this.modelRenderer.tesselateBlock(level, this.singleThreadPartList, blockState, pos, poseStack, buffer, true, OverlayTexture.NO_OVERLAY);
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
        method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
        at = @At("HEAD"),
        remap = false
    )
    private void renderBreakingTextureHead(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
    }

    @Inject(
        method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void renderBreakingTextureTail(CallbackInfo ci){
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
    }

    @ModifyVariable(
        method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getSeed(Lnet/minecraft/core/BlockPos;)J"
        ),
        remap = false
    )
    private ModelData renderBreakingTextureUpdateModelData(ModelData modelData, BlockState state, BlockPos pos, BlockAndTintGetter level){
        /*
            When rendering the breaking overlay, Forge only uses the model data acquired from the block entity.
            This mixin updates the model data through the block model like is the case in other places where model data is used.
         */
        return this.blockModelShaper.getBlockModel(state).getModelData(level, pos, state, modelData);
    }
}
