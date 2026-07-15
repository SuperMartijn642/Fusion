package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.MixinReEntrancePreventer;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Created 16/07/2026 by SuperMartijn642
 */
@Mixin(ForgeBlockModelRenderer.class)
public class ForgeBlockModelRendererMixin {

    @Unique
    private static final ThreadLocal<ModelsByRandomOffset> MODELS_BY_RANDOM_OFFSET = ThreadLocal.withInitial(ModelsByRandomOffset::new);

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void collectModelsByRandomOffset(VertexLighterFlat lighter, IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci){
        MixinReEntrancePreventer.forgeBlockModelRendererMixin$collectModelsByRandomOffset(lighter, level, model, state, pos, buffer, cull, random, seed, modelData, ci, MODELS_BY_RANDOM_OFFSET);
    }
}
