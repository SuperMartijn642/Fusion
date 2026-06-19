package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ItemRenderContext;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinder;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ItemRenderContext.class)
public class ItemRenderContextMixinSodium {

    @Inject(
        method = "tintQuad",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void tintQuad(MutableQuadViewWrapper quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            SodiumSpriteFinder spriteFinder = quad.atlas() == QuadAtlas.ITEM ?
                SpriteFinderCache.forItemAtlas() : SpriteFinderCache.forBlockAtlas();
            TextureAtlasSprite sprite = quad.getOriginal().sprite(spriteFinder);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = 0xFF000000 | QuadTintingHelper.getInWorldColor(tinting, null, null, null);
                    for(int i = 0; i < 4; i++)
                        quad.multiplyColor(color);
                    ci.cancel();
                }
            }
        }
    }
}
