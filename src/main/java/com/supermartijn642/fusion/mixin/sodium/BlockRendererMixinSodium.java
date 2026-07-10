package com.supermartijn642.fusion.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public class BlockRendererMixinSodium {

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();
    @Unique
    private BlockRenderContext dummyRenderContext;
    @Unique
    private final BlockPos.MutableBlockPos dummyOrigin = new BlockPos.MutableBlockPos();

    @Shadow
    private boolean renderModel(BlockRenderContext context, ChunkModelBuilder buffers){
        throw new AssertionError();
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void renderModel(BlockRenderContext context, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> ci){
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
        this.modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    this.dummyRenderContext.update(pos, this.dummyOrigin, state, entry, seed);
                    if(this.renderModel(this.dummyRenderContext, buffers))
                        rendered.set(true);
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.setReturnValue(rendered.get());
    }

    @ModifyExpressionValue(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original, @Local BlockRenderContext context){
        if(!(context.model() instanceof ModelsByRandomOffset.Entry entry))
            return original;
        Vector3fc offset = entry.getOffset();
        return new Vec3(offset.x(), offset.y(), offset.z());
    }

    @Inject(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/blender/ColorBlender;getColors(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/blender/ColorSampler;Ljava/lang/Object;)[I"
        )
    )
    private void getBlockTint(BlockRenderContext ctx, LightPipeline lighter, Vec3 offset, ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace, CallbackInfo ci, @Local BakedQuad quad, @Local LocalRef<int[]> colors){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, ctx.state(), ctx.world(), ctx.pos()), 255);
                    colors.set(new int[]{color, color, color, color});
                }
            }
        }
    }
}
