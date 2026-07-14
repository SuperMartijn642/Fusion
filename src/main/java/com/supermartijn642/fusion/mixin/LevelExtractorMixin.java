package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.BlockBreakingStateExtension;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 09/07/2026 by SuperMartijn642
 */
@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

    @Shadow
    private @Nullable ClientLevel level;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();
    @Unique
    private final RandomSource randomSource = RandomSource.create();

    @ModifyExpressionValue(
        method = "extractBlockDestroyAnimation",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/client/renderer/state/level/BlockBreakingRenderState;"
        )
    )
    private BlockBreakingRenderState extractBlockDestroyAnimation(BlockBreakingRenderState breakingState, @Share("parts") LocalRef<List<BlockStateModelPart>> dummyPartsList){
        BlockState blockState = breakingState.blockState();
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        if(!(model instanceof BlockModelModifierBakedModel))
            return breakingState;
        BlockPos pos = breakingState.blockPos();
        List<Pair<Vector3fc,List<BlockStateModelPart>>> parts = new ArrayList<>();
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.set(true);
        List<BlockStateModelPart> partsList = dummyPartsList.get();
        if(partsList == null)
            dummyPartsList.set(partsList = new ArrayList<>());
        long seed = blockState.getSeed(pos);
        this.modelsByRandomOffset.setContext(pos, blockState.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.level, pos, blockState);
            List<BlockStateModelPart> finalPartsList = partsList;
            this.modelsByRandomOffset.foreach(
                entry -> {
                    this.randomSource.setSeed(seed);
                    entry.collectParts(this.level, pos, blockState, this.randomSource, finalPartsList);
                    parts.add(Pair.of(entry.getOffset(), List.copyOf(finalPartsList)));
                    finalPartsList.clear();
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        FusionClient.IS_RENDERING_BREAKING_OVERLAY.remove();
        //noinspection DataFlowIssue
        ((BlockBreakingStateExtension)(Object)breakingState).setFusionParts(parts);
        return breakingState;
    }
}
