package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.BitSet;
import java.util.List;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

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
