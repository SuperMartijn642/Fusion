package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.MixinReEntrancePreventer;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

    @Unique
    private final ThreadLocal<ModelsByRandomOffset> modelsByRandomOffset = ThreadLocal.withInitial(ModelsByRandomOffset::new);

    @Inject(
        method = "renderModelSmooth(Lnet/minecraft/world/IEnviromentBlockReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZLjava/util/Random;JLnet/minecraftforge/client/model/data/IModelData;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void collectModelsByRandomOffsetWithAO(IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci){
        //noinspection DataFlowIssue
        BlockModelRenderer self = (BlockModelRenderer)(Object)this;
        MixinReEntrancePreventer.modelBlockRendererMixin$collectModelsByRandomOffsetWithAO(self, level, model, state, pos, buffer, cull, random, seed, modelData, ci, this.modelsByRandomOffset);
    }

    @Inject(
        method = "renderModelFlat(Lnet/minecraft/world/IEnviromentBlockReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZLjava/util/Random;JLnet/minecraftforge/client/model/data/IModelData;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void collectModelsByRandomOffsetWithoutAO(IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci){
        //noinspection DataFlowIssue
        BlockModelRenderer self = (BlockModelRenderer)(Object)this;
        MixinReEntrancePreventer.modelBlockRendererMixin$collectModelsByRandomOffsetWithoutAO(self, level, model, state, pos, buffer, cull, random, seed, modelData, ci, this.modelsByRandomOffset);
    }

    @ModifyVariable(
        method = "renderModelFaceAO(Lnet/minecraft/world/IEnviromentBlockReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;Ljava/util/List;[FLjava/util/BitSet;Lnet/minecraft/client/renderer/BlockModelRenderer$AmbientOcclusionFace;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/BlockState;getOffset(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d modifyRandomOffsetWithAO(Vec3d original, IEnviromentBlockReader level, BlockState state, BlockPos pos, BufferBuilder buffer, List<BakedQuad> quads, float[] arr, BitSet o, BlockModelRenderer.AmbientOcclusionFace o2){
        Vector3f offset = ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.get();
        if(offset == null)
            return original;
        return new Vec3d(offset.x(), offset.y(), offset.z());
    }

    @ModifyVariable(
        method = "renderModelFaceFlat(Lnet/minecraft/world/IEnviromentBlockReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;IZLnet/minecraft/client/renderer/BufferBuilder;Ljava/util/List;Ljava/util/BitSet;)V",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/BlockState;getOffset(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d modifyRandomOffsetWithoutAO(Vec3d original, IEnviromentBlockReader level, BlockState state, BlockPos pos, int lighting, boolean calculateFacing, BufferBuilder buffer, List<BakedQuad> quads, BitSet o){
        Vector3f offset = ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.get();
        if(offset == null)
            return original;
        return new Vec3d(offset.x(), offset.y(), offset.z());
    }

    @ModifyVariable(
        method = "renderModelFaceAO",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;postProcessFacePosition(DDD)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private BakedQuad tintQuadAO(BakedQuad quad, IEnviromentBlockReader level, BlockState state, BlockPos pos, BufferBuilder vertexConsumer, List<BakedQuad> quads, float[] arr, BitSet bitSet, BlockModelRenderer.AmbientOcclusionFace occlusion){
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
                    vertexConsumer.faceTint(occlusion.brightness[0] * red, occlusion.brightness[0] * green, occlusion.brightness[0] * blue, 4);
                    vertexConsumer.faceTint(occlusion.brightness[1] * red, occlusion.brightness[1] * green, occlusion.brightness[1] * blue, 3);
                    vertexConsumer.faceTint(occlusion.brightness[2] * red, occlusion.brightness[2] * green, occlusion.brightness[2] * blue, 2);
                    vertexConsumer.faceTint(occlusion.brightness[3] * red, occlusion.brightness[3] * green, occlusion.brightness[3] * blue, 1);
                }
            }
        }
        return quad;
    }

    @ModifyVariable(
        method = "renderModelFaceFlat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BufferBuilder;postProcessFacePosition(DDD)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private BakedQuad tintQuadFlat(BakedQuad quad, IEnviromentBlockReader level, BlockState state, BlockPos pos, int lightmap, boolean recalculateLight, BufferBuilder vertexConsumer){
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
                    vertexConsumer.faceTint(red, green, blue, 4);
                    vertexConsumer.faceTint(red, green, blue, 3);
                    vertexConsumer.faceTint(red, green, blue, 2);
                    vertexConsumer.faceTint(red, green, blue, 1);
                }
            }
        }
        return quad;
    }
}
