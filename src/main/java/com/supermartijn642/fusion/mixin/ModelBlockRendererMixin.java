package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    // In case texture has a custom tinting set, replace the original tinting
    @WrapOperation(method = "putQuadWithTint", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;getTintColor(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;I)I"))
    private int getTintColor(ModelBlockRenderer instance, BlockAndTintGetter level, BlockState state, BlockPos pos, int tintIndex, Operation<Integer> original, @Local(argsOnly = true, name = "quad") BakedQuad quad){
        if (tintIndex == 39216 && quad.materialInfo().sprite() instanceof BaseTextureSprite sprite){
            BaseTextureData.QuadTinting tinting = sprite.data().getTinting();
            if (tinting != null)
                return QuadTintingHelper.getColor(tinting, state, level, pos);
        }
        return original.call(instance, level, state, pos, tintIndex);
    }
}
