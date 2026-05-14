package com.supermartijn642.fusion.mixin.fabric;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererImplMixin {

    @Shadow
    private BlockAndTintGetter level;
    @Shadow
    private BlockPos pos;
    @Shadow
    private BlockState blockState;

    @WrapWithCondition(
        method = "tintQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;multiplyColor(I)Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;"
        )
    )
    private boolean tintQuad(MutableQuadView quad, int originalTint){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(quad);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int tint = QuadTintingHelper.getInWorldColor(tinting, this.blockState, this.level, this.pos);
                    quad.multiplyColor(tint);
                }
            }
            return false;
        }
        return true;
    }
}
