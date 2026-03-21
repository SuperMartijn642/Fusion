package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(DiggingParticle.class)
public abstract class DiggingParticleMixin extends SpriteTexturedParticle {

    @Final
    @Shadow
    private BlockState blockState;

    protected DiggingParticleMixin(ClientWorld level, double x, double y, double z){
        super(level, x, y, z);
    }

    @Inject(
        method = "multiplyColor",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tintQuad(BlockPos pos, CallbackInfo ci){
        // If the texture has a custom tinting set, replace the original tinting
        TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(this.sprite);
        if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
            BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
            if(tinting != null){
                int color = QuadTintingHelper.getColor(tinting, this.blockState, this.level, pos);
                this.rCol *= (color >> 16 & 0xFF) / 255f;
                this.gCol *= (color >> 8 & 0xFF) / 255f;
                this.bCol *= (color & 0xFF) / 255f;
                ci.cancel();
            }
        }
    }
}
