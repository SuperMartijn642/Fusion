package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
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

    private final QuadInstance quadInstance = new QuadInstance();

    @Unique
    private final BitSet fusionTintsComputed = new BitSet();
    @Unique
    private final IntList fusionTintValues = new IntArrayList(BaseTextureData.QuadTinting.values().length);

    private ModelBlockRendererMixin(){
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
