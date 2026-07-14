package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    @Final
    @Shadow
    private QuadInstance quadInstance;

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();
    @Unique
    private final BitSet fusionTintsComputed = new BitSet();
    @Unique
    private final IntList fusionTintValues = new IntArrayList(BaseTextureData.QuadTinting.values().length);

    private ModelBlockRendererMixin(){
    }

    @Shadow
    private void tesselateBlock(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed){
        throw new AssertionError();
    }

    @Inject(
        method = "tesselateBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void collectModelsByOffset(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState state, BlockStateModel model, long seed, CallbackInfo ci){
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

    @Inject(
        method = "resetTintCache",
        at = @At("HEAD")
    )
    private void resetTintCache(CallbackInfo ci){
        this.fusionTintsComputed.clear();
    }

    @WrapWithCondition(
        method = "putQuadWithTint",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;multiplyColor(I)V"
        )
    )
    private boolean tintQuadCached(QuadInstance quadInstance, int originalTint, @Local BlockAndTintGetter level, @Local BlockState state, @Local BlockPos pos, @Local BakedQuad quad){
        // Intercept quads that have a custom Fusion tinting
        if(quad.materialInfo().tintIndex() == 39216){
            // Get the sprite instance
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                // Get custom tinting
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    // Get or compute tint value
                    int tint;
                    if(tinting.ordinal() < this.fusionTintsComputed.size() && this.fusionTintsComputed.get(tinting.ordinal()))
                        tint = this.fusionTintValues.getInt(tinting.ordinal());
                    else{
                        tint = QuadTintingHelper.getInWorldColor(tinting, state, level, pos);
                        if(this.fusionTintValues.size() <= tinting.ordinal()){
                            while(this.fusionTintValues.size() < tinting.ordinal())
                                this.fusionTintValues.add(0);
                            this.fusionTintValues.add(tint);
                        }else
                            this.fusionTintValues.set(tinting.ordinal(), tint);
                        this.fusionTintsComputed.set(tinting.ordinal());
                    }
                    // Apply tint
                    this.quadInstance.multiplyColor(tint);
                    return false;
                }
            }
        }
        return true;
    }
}
