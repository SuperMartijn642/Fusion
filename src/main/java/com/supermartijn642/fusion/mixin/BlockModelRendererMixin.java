package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 26/05/2023 by SuperMartijn642
 */
@Mixin(value = BlockModelRenderer.class, priority = 900)
public class BlockModelRendererMixin {

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD")
    )
    private void renderModelSmoothHead(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        if(model instanceof ConnectingBakedModel)
            ((ConnectingBakedModel)model).levelCapture.set(Pair.of(level, pos));
    }

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN")
    )
    private void renderModelSmoothTail(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        if(model instanceof ConnectingBakedModel)
            ((ConnectingBakedModel)model).levelCapture.set(null);
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD")
    )
    private void renderModelFlatHead(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        if(model instanceof ConnectingBakedModel)
            ((ConnectingBakedModel)model).levelCapture.set(Pair.of(level, pos));
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN")
    )
    private void renderModelFlatTail(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        if(model instanceof ConnectingBakedModel)
            ((ConnectingBakedModel)model).levelCapture.set(null);
    }
}
