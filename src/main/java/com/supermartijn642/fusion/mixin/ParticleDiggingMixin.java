package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(ParticleDigging.class)
public abstract class ParticleDiggingMixin extends Particle {

    @Final
    @Shadow
    private IBlockState sourceState;

    protected ParticleDiggingMixin(World level, double x, double y, double z){
        super(level, x, y, z);
    }


    @Inject(
        method = "multiplyColor",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tintQuad(BlockPos pos, CallbackInfo ci){
        // If the texture has a custom tinting set, replace the original tinting
        TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(this.particleTexture);
        if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
            BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
            if(tinting != null){
                int color = QuadTintingHelper.getColor(tinting, this.sourceState, this.world, pos);
                this.particleRed *= (color >> 16 & 0xFF) / 255f;
                this.particleGreen *= (color >> 8 & 0xFF) / 255f;
                this.particleBlue *= (color & 0xFF) / 255f;
                ci.cancel();
            }
        }
    }
}
