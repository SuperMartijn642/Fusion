package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(AbstractTerrainRenderContext.class)
public class AbstractTerrainRenderContextMixin {

    @Final
    @Shadow(remap = false)
    private BlockRenderInfo blockInfo;

    @Inject(
        method = "tintQuad",
        at = @At("RETURN"),
        remap = false
    )
    private void tintQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite;
            if(quad.tag() == 0)
                sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(quad);
            else{
                float u = ((quad.tag() >> 4) & 16383) / 16383f;
                float v = (quad.tag() >> 18) / 16383f;
                sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(u, v);
            }
            if(sprite instanceof BaseTextureSprite){
                BaseTextureData.QuadTinting tinting = ((BaseTextureSprite)sprite).data().getTinting();
                if(tinting != null){
                    int tint = QuadTintingHelper.getColor(tinting, this.blockInfo.blockState, this.blockInfo.blockView, this.blockInfo.blockPos);
                    for(int i = 0; i < 4; i++)
                        quad.color(i, net.minecraft.util.ARGB.multiply(quad.color(i), tint));
                }
            }
        }
    }
}
