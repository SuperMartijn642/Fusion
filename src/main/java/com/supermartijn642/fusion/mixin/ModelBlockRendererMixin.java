package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(BlockModelRenderer.class)
public class ModelBlockRendererMixin {

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
