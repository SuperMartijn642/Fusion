package com.supermartijn642.fusion.mixin.rubidium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
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
public class ItemRendererMixinRubidium {

    @Redirect(
        method = "sodium$renderBakedItemQuads",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/vertex/formats/ModelVertex;writeQuadVertices(Lme/jellysquid/mods/sodium/client/render/vertex/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;III)V",
            remap = false
        )
    )
    private void renderQuadList(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int light, int overlay, int color){
        // In case texture has a custom tinting set, replace the original tinting
        if(((BakedQuad)quad).tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null)
                    color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, null, null, null), 255);
            }
        }
        // Call the original method
        ModelVertex.writeQuadVertices(writer, matrices, quad, light, overlay, color);
    }
}
