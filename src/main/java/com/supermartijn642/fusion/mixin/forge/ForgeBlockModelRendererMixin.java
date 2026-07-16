package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.BlockInfoExtension;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 26/05/2023 by SuperMartijn642
 */
@Mixin(value = ForgeBlockModelRenderer.class, priority = 900)
public class ForgeBlockModelRendererMixin {

    @Unique
    private static final ThreadLocal<ModelsByRandomOffset> MODELS_BY_RANDOM_OFFSET = ThreadLocal.withInitial(ModelsByRandomOffset::new);

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void collectModelsByRandomOffset(VertexLighterFlat lighter, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, long seed, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = MODELS_BY_RANDOM_OFFSET.get();
        BlockInfoExtension blockInfo = (BlockInfoExtension)((VertexLighterFlatExtension)lighter).fusionGetBlockInfo();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    blockInfo.setFusionOffsetOverwrite(entry.getOffset());
                    if(ForgeBlockModelRenderer.render(lighter, level, entry, state, pos, buffer, cull, seed))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            blockInfo.setFusionOffsetOverwrite(null);
        }
        ci.setReturnValue(rendered.get());
    }

    @Inject(
        method = "render(Lnet/minecraftforge/client/model/pipeline/VertexLighterFlat;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD"),
        remap = false
    )
    private static void renderHead(VertexLighterFlat lighter, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.set(new BlockRenderContext(level, pos, state));
    }

    @Inject(
        method = "render(Lnet/minecraftforge/client/model/pipeline/VertexLighterFlat;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN"),
        remap = false
    )
    private static void renderTail(VertexLighterFlat lighter, IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.remove();
    }
}
