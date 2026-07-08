package com.supermartijn642.fusion.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererImplMixin {

    @Shadow
    private BlockAndTintGetter level;
    @Shadow
    private BlockPos pos;
    @Shadow
    private BlockState blockState;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    @Shadow
    private void tesselateBlock(QuadEmitter output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed){
        throw new AssertionError();
    }

    @Inject(
        method = "tesselateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByOffset(QuadEmitter output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed, CallbackInfo ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        this.modelsByRandomOffset.setContext(pos, state.getOffset(pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> this.tesselateBlock(output, x, y, z, level, pos, state, entry, seed)
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.cancel();
    }

    @ModifyExpressionValue(
        method = "tesselateBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getOffset(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 modifyRandomOffset(Vec3 original, @Local BlockStateModel model){
        if(model instanceof ModelsByRandomOffset.Entry entry){
            Vector3fc offset = entry.getOffset();
            return new Vec3(offset.x(), offset.y(), offset.z());
        }
        return original;
    }

    @WrapWithCondition(
        method = "tintQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;multiplyColor(I)Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/MutableQuadView;"
        )
    )
    private boolean tintQuad(MutableQuadView quad, int originalTint){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.tintIndex() == 39216){
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlases.getBlocks()).spriteFinder().find(quad);
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int tint = QuadTintingHelper.getInWorldColor(tinting, this.blockState, this.level, this.pos);
                    quad.multiplyColor(tint);
                }
            }
            return false;
        }
        return true;
    }
}
