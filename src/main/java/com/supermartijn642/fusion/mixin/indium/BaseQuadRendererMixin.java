package com.supermartijn642.fusion.mixin.indium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import link.infra.indium.renderer.mesh.MutableQuadViewImpl;
import link.infra.indium.renderer.render.BaseQuadRenderer;
import link.infra.indium.renderer.render.BlockRenderInfo;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 04/01/2025 by SuperMartijn642
 */
@Mixin(BaseQuadRenderer.class)
public class BaseQuadRendererMixin {

    @Final
    @Shadow(remap = false)
    private BlockRenderInfo blockInfo;

    @ModifyVariable(
        method = "colorizeQuad",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Llink/infra/indium/renderer/render/BlockRenderInfo;blockColor(I)I",
            shift = At.Shift.AFTER
        ),
        ordinal = 1,
        remap = false
    )
    private int colorizeQuad(int blockColor, MutableQuadViewImpl quad, int colorIndex){
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
                if(tinting != null)
                    return QuadTintingHelper.getColor(tinting, this.blockInfo.blockState, this.blockInfo.blockView, this.blockInfo.blockPos);
            }
        }
        return blockColor;
    }
}
