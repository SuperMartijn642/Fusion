package com.supermartijn642.fusion.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin extends AbstractBlockRenderContext {

    @Final
    @Shadow
    private Vector3f posOffset;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    private BlockRendererMixin(){
    }

    @Shadow
    private void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin){
        throw new AssertionError();
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD"),
        cancellable = true
    )
    private void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        this.modelsByRandomOffset.setContext(pos, state.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, this.level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> this.renderModel(entry, state, pos, origin)
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.cancel();
    }

    @ModifyExpressionValue(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;hasOffsetFunction()Z"
        )
    )
    private boolean modifyRandomOffset(boolean original, @Local BlockStateModel model){
        if(model instanceof ModelsByRandomOffset.Entry entry){
            Vector3fc offset = entry.getOffset();
            this.posOffset.add(offset.x(), offset.y(), offset.z());
            return false;
        }
        return original;
    }

    @Inject(
        method = "tintQuad",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void tintQuad(MutableQuadViewImpl quad, CallbackInfo ci){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = -16777216 | QuadTintingHelper.getColor(tinting, this.state, this.slice, this.pos);
                    for(int i = 0; i < 4; ++i)
                        quad.setColor(i, ColorMixer.mulComponentWise(color, quad.getColor(i)));
                    ci.cancel();
                }
            }
        }
    }
}
