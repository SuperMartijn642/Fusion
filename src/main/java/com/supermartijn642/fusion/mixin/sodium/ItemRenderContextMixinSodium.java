package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ItemRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.ExtendedTextureAtlas;
import net.minecraft.client.Minecraft;
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
    private void tintQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.cachedSprite();
            if(!(sprite instanceof BaseTextureSprite)){
                if(quad.getTag() == 0)
                    sprite = quad.sprite(((ExtendedTextureAtlas)Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks())).sodium$getSpriteFinder());
                else{
                    float u = ((quad.getTag() >> 4) & 16383) / 16383f;
                    float v = (quad.getTag() >> 18) / 16383f;
                    sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(u, v);
                    quad.cachedSprite(sprite);
                }
            }
            if(sprite instanceof BaseTextureSprite){
                BaseTextureData.QuadTinting tinting = ((BaseTextureSprite)sprite).data().getTinting();
                if(tinting != null){
                    int color = 0xFF000000 | QuadTintingHelper.getColor(tinting, null, null, null);
                    for(int i = 0; i < 4; i++)
                        quad.setColor(i, ColorMixer.mulComponentWise(color, quad.getColor(i)));
                    ci.cancel();
                }
            }
        }
    }
}
