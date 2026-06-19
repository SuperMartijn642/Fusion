package com.supermartijn642.fusion.mixin.sodium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ExtendedBlockModelFeatureRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinder;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 19/06/2026 by SuperMartijn642
 */
@Mixin(ExtendedBlockModelFeatureRenderer.class)
public class ExtendedBlockModelFeatureRendererMixinSodium {

    @Inject(
        method = "bufferQuad",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bufferQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            SodiumSpriteFinder spriteFinder = quad.getQuadAtlas() == SodiumQuadAtlas.ITEM ?
                SpriteFinderCache.forItemAtlas() : SpriteFinderCache.forBlockAtlas();
            TextureAtlasSprite sprite = quad.sprite(spriteFinder);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int tint = QuadTintingHelper.getDefaultColor(tinting, Blocks.AIR.defaultBlockState());
                    ((ExtendedMutableQuadViewImpl)quad).getWrapper().multiplyColor(tint);
                }
            }
        }
    }
}
