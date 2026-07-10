package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @ModifyExpressionValue(
        method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/function/Function;ZI)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original){
        Vector3fc offset = ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.get();
        if(offset != null)
            return new Vec3(offset.x(), offset.y(), offset.z());
        return original;
    }

    @ModifyVariable(
        method = "putQuadData",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer$CommonRenderStorage;tintCacheValue:I",
            shift = At.Shift.AFTER
        ),
        ordinal = 2
    )
    private int tintQuadCached(int blockColor, BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = quad.sprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null)
                    return QuadTintingHelper.getColor(tinting, state, level, pos);
            }
        }
        return blockColor;
    }

    @ModifyVariable(
        method = "putQuadData",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/color/block/BlockColors;getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",
            shift = At.Shift.AFTER
        ),
        ordinal = 2
    )
    private int tintQuad(int blockColor, BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = quad.sprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null)
                    return QuadTintingHelper.getColor(tinting, state, level, pos);
            }
        }
        return blockColor;
    }
}
