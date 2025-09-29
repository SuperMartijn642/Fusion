package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin extends AbstractBlockRenderContext {

    @Inject(
        method = "tintQuad",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void tintQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite;
            if(quad.tag() == 0)
                sprite = quad.sprite(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder());
            else{
                float u = (quad.tag() & 65535) / 65535f;
                float v = (quad.tag() >> 16) / 65535f;
                sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(u, v);
                quad.cachedSprite(sprite);
            }
            if(sprite instanceof BaseTextureSprite){
                BaseTextureData.QuadTinting tinting = ((BaseTextureSprite)sprite).data().getTinting();
                if(tinting != null){
                    int color = -16777216 | QuadTintingHelper.getColor(tinting, this.state, this.slice, this.pos);
                    for(int i = 0; i < 4; ++i)
                        quad.color(i, ColorMixer.mulComponentWise(color, quad.color(i)));
                    ci.cancel();
                }
            }
        }
    }
}
