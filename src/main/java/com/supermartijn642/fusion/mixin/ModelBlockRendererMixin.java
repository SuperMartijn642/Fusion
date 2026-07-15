package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(BlockModelRenderer.class)
public class ModelBlockRendererMixin {

    @Unique
    private final ThreadLocal<ModelsByRandomOffset> modelsByRandomOffset = ThreadLocal.withInitial(ModelsByRandomOffset::new);

    @Shadow
    private boolean renderModel(IBlockDisplayReader level, IBakedModel model, BlockState state, BlockPos pos, MatrixStack poseStack, IVertexBuilder buffer, boolean cull, Random random, long seed, int overlay, IModelData entityData){
        throw new AssertionError();
    }

    @Inject(
        method = "renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void collectModelsByRandomOffset(IBlockDisplayReader level, IBakedModel model, BlockState state, BlockPos pos, MatrixStack poseStack, IVertexBuilder buffer, boolean cull, Random random, long seed, int overlay, IModelData modelData, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = this.modelsByRandomOffset.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    poseStack.pushPose();
                    if(this.renderModel(level, entry, state, pos, poseStack, buffer, cull, random, seed, overlay, modelData))
                        rendered.set(true);
                    poseStack.popPose();
                }
            );
        }finally{
            modelsByRandomOffset.reset();
        }
        ci.setReturnValue(rendered.get());
    }

    @Inject(
        method = "renderModel(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/BlockState;getOffset(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/vector/Vector3d;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        remap = false
    )
    private void modifyRandomOffset(IBlockDisplayReader level, IBakedModel model, BlockState state, BlockPos pos, MatrixStack poseStack, IVertexBuilder buffer, boolean cull, Random random, long seed, int overlay, IModelData modelData, CallbackInfoReturnable<Boolean> ci, boolean ambientOcclusion, Vector3d defaultOffset){
        if(!(model instanceof ModelsByRandomOffset.Entry))
            return;
        ModelsByRandomOffset.Entry entry = (ModelsByRandomOffset.Entry)model;
        Vector3f offset = entry.getOffset();
        poseStack.translate(offset.x() - defaultOffset.x(), offset.y() - defaultOffset.y(), offset.z() - defaultOffset.z());
    }

    @ModifyVariable(
        method = "putQuadData",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/renderer/color/BlockColors;getColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;I)I",
            shift = At.Shift.AFTER
        ),
        ordinal = 5
    )
    private int tintQuad(int blockColor, IBlockDisplayReader level, BlockState state, BlockPos pos, IVertexBuilder vertexConsumer, MatrixStack.Entry pose, BakedQuad quad, float red, float green, float blue, float alpha, int lighting1, int lighting2, int lighting3, int lighting4, int overlay){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null)
                    return QuadTintingHelper.getColor(tinting, state, level, pos);
            }
        }
        return blockColor;
    }
}
