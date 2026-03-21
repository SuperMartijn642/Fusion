package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(ItemRenderContext.class)
public class ItemRenderContextMixin {

    @Inject(
        method = "tintQuad",
        at = @At("TAIL"),
        remap = false
    )
    private void tintQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks()).spriteFinder().find(quad);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    for(int i = 0; i < 4; i++)
                        quad.color(i, ARGB.multiply(color, quad.color(i)));
                }
            }
        }
    }
}
