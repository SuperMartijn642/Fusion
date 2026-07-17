package com.supermartijn642.fusion.mixin.indium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import link.infra.indium.renderer.render.AbstractBlockRenderContext;
import link.infra.indium.renderer.render.TerrainRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 10/07/2026 by SuperMartijn642
 */
@Mixin(TerrainRenderContext.class)
public abstract class TerrainRenderContextMixinIndium extends AbstractBlockRenderContext {

    @Shadow
    private boolean didOutput;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();
    @Unique
    private BlockRenderContext dummyRenderContext;
    @Unique
    private final BlockPos.MutableBlockPos dummyOrigin = new BlockPos.MutableBlockPos();

    private TerrainRenderContextMixinIndium(){
    }

    @Shadow
    private boolean tessellateBlock(BlockRenderContext context){
        throw new AssertionError();
    }

    @Inject(
        method = "tessellateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffset(BlockRenderContext context, CallbackInfoReturnable<Boolean> ci){
        BakedModel model = context.model();
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        BlockAndTintGetter level = context.world();
        if(this.dummyRenderContext == null || this.dummyRenderContext.world() != level)
            this.dummyRenderContext = new BlockRenderContext(level);
        Vector3fc origin = context.origin();
        this.dummyOrigin.set(origin.x(), origin.y(), origin.z());
        BlockPos pos = context.pos();
        BlockState state = context.state();
        long seed = context.seed();
        AtomicBoolean rendered = new AtomicBoolean(false);
        this.modelsByRandomOffset.setContext(pos, state.getOffset(this.blockInfo.blockView, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.blockInfo.blockView, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    this.dummyRenderContext.update(pos, this.dummyOrigin, state, entry, seed);
                    this.tessellateBlock(this.dummyRenderContext);
                    if(this.didOutput)
                        rendered.set(true);
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        this.didOutput = rendered.get();
        ci.setReturnValue(rendered.get());
    }

    @ModifyExpressionValue(
        method = "tessellateBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original, @Local BlockRenderContext context){
        if(context.model() instanceof ModelsByRandomOffset.Entry entry){
            Vector3fc offset = entry.getOffset();
            return new Vec3(offset.x(), offset.y(), offset.z());
        }
        return original;
    }
}
