package com.supermartijn642.fusion.mixin.embeddium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(value = ItemRenderer.class, priority = 1001)
public class ItemRendererMixinEmbeddium {

    @Redirect(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;getColor(I)I",
            remap = false
        )
    )
    private int renderQuadList(ModelQuadView quad, int vertex){
        // In case texture has a custom tinting set, replace the original tinting
        if(((BakedQuad)quad).getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = QuadTintingHelper.getColor(tinting, null, null, null);
                    return ModelQuadUtil.mixARGBColors(quad.getColor(vertex), ColorARGB.toABGR(color));
                }
            }
        }
        // Call the original method
        return quad.getColor(vertex);
    }
}
