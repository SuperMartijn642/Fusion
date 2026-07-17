package com.supermartijn642.fusion.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
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
public abstract class TerrainRenderContextMixin extends AbstractTerrainRenderContext {

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private TerrainRenderContextMixin(){
    }

    @Shadow
    private void bufferModel(BlockStateModel model, BlockState state, BlockPos pos){
        throw new AssertionError();
    }

    @Inject(
        method = "bufferModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffset(BlockStateModel model, BlockState state, BlockPos pos, CallbackInfo ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        this.modelsByRandomOffset.setContext(pos, state.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.blockInfo.blockView, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> this.bufferModel(entry, state, pos)
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.cancel();
    }

    @ModifyExpressionValue(
        method = "bufferModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original, @Local BlockStateModel model){
        if(model instanceof ModelsByRandomOffset.Entry entry){
            Vector3fc offset = entry.getOffset();
            return new Vec3(offset.x(), offset.y(), offset.z());
        }
        return original;
    }
}
