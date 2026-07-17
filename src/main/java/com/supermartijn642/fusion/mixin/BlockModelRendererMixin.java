package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 26/05/2023 by SuperMartijn642
 */
@Mixin(value = BlockModelRenderer.class, priority = 900)
public class BlockModelRendererMixin {

    @Unique
    private final ThreadLocal<ModelsByRandomOffset> modelsByRandomOffset = ThreadLocal.withInitial(ModelsByRandomOffset::new);

    private BlockModelRendererMixin(){
    }

    @Shadow
    private boolean renderModelSmooth(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, long seed){
        throw new AssertionError();
    }

    @Shadow
    private boolean renderModelFlat(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, long seed){
        throw new AssertionError();
    }

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffsetWithAO(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, long seed, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = this.modelsByRandomOffset.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    if(this.renderModelSmooth(level, entry, state, pos, buffer, cull, seed))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        ci.setReturnValue(rendered.get());
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByRandomOffsetWithoutAO(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, long seed, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = this.modelsByRandomOffset.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    if(this.renderModelFlat(level, entry, state, pos, buffer, cull, seed))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        ci.setReturnValue(rendered.get());
    }

    @ModifyVariable(
        method = "renderQuadsSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Ljava/util/List;[FLjava/util/BitSet;Lnet/minecraft/client/renderer/BlockModelRenderer$AmbientOcclusionFace;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/state/IBlockState;getOffset(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d modifyRandomOffsetWithAO(Vec3d original, IBlockAccess level, IBlockState state, BlockPos pos, BufferBuilder buffer, List<BakedQuad> quads, float[] arr, BitSet o, BlockModelRenderer.AmbientOcclusionFace o2){
        Vector3f offset = ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.get();
        if(offset == null)
            return original;
        return new Vec3d(offset.x, offset.y, offset.z);
    }

    @ModifyVariable(
        method = "renderQuadsFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;IZLnet/minecraft/client/renderer/BufferBuilder;Ljava/util/List;Ljava/util/BitSet;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/state/IBlockState;getOffset(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d modifyRandomOffsetWithoutAO(Vec3d original, IBlockAccess level, IBlockState state, BlockPos pos, int lighting, boolean calculateFacing, BufferBuilder buffer, List<BakedQuad> quads, BitSet o){
        Vector3f offset = ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.get();
        if(offset == null)
            return original;
        return new Vec3d(offset.x, offset.y, offset.z);
    }

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD")
    )
    private void renderModelSmoothHead(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.set(new BlockRenderContext(level, pos, state));
    }

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN")
    )
    private void renderModelSmoothTail(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.remove();
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("HEAD")
    )
    private void renderModelFlatHead(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.set(new BlockRenderContext(level, pos, state));
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
        at = @At("RETURN")
    )
    private void renderModelFlatTail(IBlockAccess level, IBakedModel model, IBlockState state, BlockPos pos, BufferBuilder buffer, boolean checkSides, long random, CallbackInfoReturnable<Boolean> ci){
        FusionClient.BLOCK_RENDER_CONTEXT.remove();
    }

    @ModifyVariable(
        method = "renderQuadsSmooth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;putPosition(DDD)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private BakedQuad tintQuadAO(BakedQuad quad, IBlockAccess level, IBlockState state, BlockPos pos, BufferBuilder vertexConsumer, List<BakedQuad> quads, float[] arr, BitSet bitSet, BlockModelRenderer.AmbientOcclusionFace occlusion){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, state, level, pos);
                    float red = (float)(color >> 16 & 255) / 255.0F;
                    float green = (float)(color >> 8 & 255) / 255.0F;
                    float blue = (float)(color & 255) / 255.0F;
                    vertexConsumer.putColorMultiplier(occlusion.vertexColorMultiplier[0] * red, occlusion.vertexColorMultiplier[0] * green, occlusion.vertexColorMultiplier[0] * blue, 4);
                    vertexConsumer.putColorMultiplier(occlusion.vertexColorMultiplier[1] * red, occlusion.vertexColorMultiplier[1] * green, occlusion.vertexColorMultiplier[1] * blue, 3);
                    vertexConsumer.putColorMultiplier(occlusion.vertexColorMultiplier[2] * red, occlusion.vertexColorMultiplier[2] * green, occlusion.vertexColorMultiplier[2] * blue, 2);
                    vertexConsumer.putColorMultiplier(occlusion.vertexColorMultiplier[3] * red, occlusion.vertexColorMultiplier[3] * green, occlusion.vertexColorMultiplier[3] * blue, 1);
                }
            }
        }
        return quad;
    }

    @ModifyVariable(
        method = "renderQuadsFlat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;putPosition(DDD)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private BakedQuad tintQuadFlat(BakedQuad quad, IBlockAccess level, IBlockState state, BlockPos pos, int lightmap, boolean recalculateLight, BufferBuilder vertexConsumer){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, state, level, pos);
                    float red = (float)(color >> 16 & 255) / 255.0F;
                    float green = (float)(color >> 8 & 255) / 255.0F;
                    float blue = (float)(color & 255) / 255.0F;
                    vertexConsumer.putColorMultiplier(red, green, blue, 4);
                    vertexConsumer.putColorMultiplier(red, green, blue, 3);
                    vertexConsumer.putColorMultiplier(red, green, blue, 2);
                    vertexConsumer.putColorMultiplier(red, green, blue, 1);
                }
            }
        }
        return quad;
    }
}
