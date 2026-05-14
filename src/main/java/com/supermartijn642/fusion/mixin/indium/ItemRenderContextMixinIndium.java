package com.supermartijn642.fusion.mixin.indium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import link.infra.indium.renderer.helper.ColorHelper;
import link.infra.indium.renderer.mesh.MutableQuadViewImpl;
import link.infra.indium.renderer.render.ItemRenderContext;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
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
public class ItemRenderContextMixinIndium {

    @Inject(
        method = "colorizeQuad",
        at = @At("TAIL"),
        remap = false
    )
    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(colorIndex == 39216){
            TextureAtlasSprite sprite = quad.cachedSprite();
            if(sprite == null){
                sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks())).find(quad, 0);
                quad.cachedSprite(sprite);
            }
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = 0xFF000000 | QuadTintingHelper.getColor(tinting, null, null, null);
                    for(int i = 0; i < 4; i++)
                        quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(color, quad.spriteColor(i, 0))));
                }
            }
        }
    }
}
