package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @ModifyVariable(
        method = "putQuadData",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/color/block/BlockColors;getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",
            shift = At.Shift.AFTER
        ),
        ordinal = 5
    )
    private int tintQuad(int blockColor, BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int lighting1, int lighting2, int lighting3, int lighting4, int overlay){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
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
