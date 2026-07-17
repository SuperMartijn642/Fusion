package com.supermartijn642.fusion.mixin.indium;

import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import link.infra.indium.renderer.render.TerrainBlockRenderInfo;
import link.infra.indium.renderer.render.TerrainRenderContext;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 10/07/2026 by SuperMartijn642
 */
@Mixin(TerrainRenderContext.class)
public abstract class TerrainRenderContextMixinIndium {

    @Final
    @Shadow
    private TerrainBlockRenderInfo blockInfo;
    @Shadow
    private Vec3 modelOffset;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private TerrainRenderContextMixinIndium(){
    }

    @Shadow
    private boolean tessellateBlock(BlockState state, BlockPos pos, BlockPos origin, BakedModel model, Vec3 modelOffset){
        throw new AssertionError();
    }

    @Inject(
        method = "tessellateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffset(BlockState state, BlockPos pos, BlockPos origin, BakedModel model, Vec3 modelOffset, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        this.modelsByRandomOffset.setContext(pos, state.getOffset(this.blockInfo.blockView, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.blockInfo.blockView, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    Vector3f offset = entry.getOffset();
                    this.tessellateBlock(state, pos, origin, entry, new Vec3(offset.x(), offset.y(), offset.z()));
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.setReturnValue(true);
    }
}
