package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public class BlockRendererMixinRubidium {

    @ModifyVariable(
        method = "writeGeometry",
        at = @At("HEAD"),
        ordinal = 0,
        remap = false
    )
    private int[] getBlockTint(int[] colors, BlockRenderContext ctx, ChunkVertexBufferBuilder vertexBuffer, IndexBufferBuilder indexBuffer, Vec3 offset, ModelQuadView quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(((BakedQuad)quad).tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, ctx.state(), ctx.world(), ctx.pos()));
                    colors = new int[]{color, color, color, color};
                }
            }
        }
        return colors;
    }
}
