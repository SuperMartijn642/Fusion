package com.supermartijn642.fusion.mixin.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 10/07/2026 by SuperMartijn642
 */
@Mixin(TerrainRenderContext.class)
public abstract class TerrainRenderContextMixin {

    @Final
    @Shadow
    private BlockRenderInfo blockInfo;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private TerrainRenderContextMixin(){
    }

    @Shadow
    private void tessellateBlock(BlockState state, BlockPos pos, BakedModel model, PoseStack poseStack){
        throw new AssertionError();
    }

    @Inject(
        method = "tessellateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffset(BlockState state, BlockPos pos, BakedModel model, PoseStack poseStack, CallbackInfo ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        // Undo normal offset
        Vec3 defaultOffset = state.getOffset(this.blockInfo.blockView, pos);
        poseStack.translate(-defaultOffset.x, -defaultOffset.y, -defaultOffset.z);
        // Render models with new offset
        this.modelsByRandomOffset.setContext(pos, state.getOffset(this.blockInfo.blockView, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.blockInfo.blockView, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    poseStack.pushPose();
                    Vector3f offset = entry.getOffset();
                    poseStack.translate(offset.x(), offset.y(), offset.z());
                    this.tessellateBlock(state, pos, entry, poseStack);
                    poseStack.popPose();
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.cancel();
    }
}
