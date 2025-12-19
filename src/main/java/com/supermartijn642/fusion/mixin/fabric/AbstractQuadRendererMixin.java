package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractQuadRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(AbstractQuadRenderer.class)
public class AbstractQuadRendererMixin {

    @Final
    @Shadow(remap = false)
    private BlockRenderInfo blockInfo;

    @ModifyVariable(
        method = "colorizeQuad",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/BlockRenderInfo;blockColor(I)I",
            shift = At.Shift.AFTER
        ),
        ordinal = 1,
        remap = false
    )
    private int colorizeQuad(int blockColor, MutableQuadViewImpl quad, int colorIndex){
        // In case texture has a custom tinting set, replace the original tinting
        if(colorIndex == 39216){
            TextureAtlasSprite sprite;
            if(quad.tag() == 0)
                sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks())).find(quad, 0);
            else{
                float u = ((quad.tag() >> 4) & 16383) / 16383f;
                float v = (quad.tag() >> 18) / 16383f;
                sprite = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks())).find(u, v);
            }
            if(sprite instanceof BaseTextureSprite){
                BaseTextureData.QuadTinting tinting = ((BaseTextureSprite)sprite).data().getTinting();
                if(tinting != null)
                    return QuadTintingHelper.getColor(tinting, this.blockInfo.blockState, this.blockInfo.blockView, this.blockInfo.blockPos);
            }
        }
        return blockColor;
    }
}
