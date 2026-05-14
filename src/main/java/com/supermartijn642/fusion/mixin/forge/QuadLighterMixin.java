package com.supermartijn642.fusion.mixin.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.lighting.QuadLighter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(QuadLighter.class)
public class QuadLighterMixin {

    @Shadow(remap = false)
    private BlockAndTintGetter level;
    @Shadow(remap = false)
    private BlockPos pos;
    @Shadow(remap = false)
    private BlockState state;
    @Final
    @Shadow(remap = false)
    private float[] cachedTintColor;

    @ModifyVariable(
        method = "process",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private float[] tintQuad(float[] color, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, int overlay){
        // If the texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int packedColor = QuadTintingHelper.getColor(tinting, this.state, this.level, this.pos);
                    this.cachedTintColor[0] = ((packedColor >> 16) & 0xFF) / 255F;
                    this.cachedTintColor[1] = ((packedColor >> 8) & 0xFF) / 255F;
                    this.cachedTintColor[2] = (packedColor & 0xFF) / 255F;
                    return this.cachedTintColor;
                }
            }
        }
        return color;
    }
}
