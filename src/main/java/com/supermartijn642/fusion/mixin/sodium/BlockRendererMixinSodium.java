package com.supermartijn642.fusion.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public class BlockRendererMixinSodium {

    @Inject(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/blender/ColorBlender;getColors(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;Lme/jellysquid/mods/sodium/client/model/quad/blender/ColorSampler;Ljava/lang/Object;)[I"
        )
    )
    private void getBlockTint(BlockRenderContext ctx, LightPipeline lighter, Vec3 offset, ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace, CallbackInfo ci, @Local BakedQuad quad, @Local LocalRef<int[]> colors){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, ctx.state(), ctx.world(), ctx.pos()), 255);
                    colors.set(new int[]{color, color, color, color});
                }
            }
        }
    }
}
