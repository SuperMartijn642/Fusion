package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 26/05/2023 by SuperMartijn642
 */
@Mixin(value = ForgeBlockModelRenderer.class, priority = 900)
public class ForgeBlockModelRendererMixin {

    @Inject(
        method = "render(Lnet/minecraftforge/client/model/pipeline/VertexLighterFlat;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD"),
        remap = false
    )
    private static void renderHead(VertexLighterFlat lighter, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        ConnectingBakedModel.levelCapture.set(Pair.of(level, pos));
    }

    @Inject(
        method = "render(Lnet/minecraftforge/client/model/pipeline/VertexLighterFlat;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN"),
        remap = false
    )
    private static void renderTail(VertexLighterFlat lighter, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        ConnectingBakedModel.levelCapture.set(null);
    }
}
