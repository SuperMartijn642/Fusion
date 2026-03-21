package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 16/09/2024 by SuperMartijn642
 */
@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin extends TextureSheetParticle {

    protected TerrainParticleMixin(ClientLevel clientLevel, double d, double e, double f){
        super(clientLevel, d, e, f);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At("TAIL")
    )
    private void tintQuad(ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, BlockState state, BlockPos pos, CallbackInfo ci){
        // If the texture has a custom tinting set, replace the original tinting
        TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(this.sprite);
        if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
            BaseTextureData.QuadTinting tinting = data.getTinting();
            if(tinting != null){
                int color = QuadTintingHelper.getColor(tinting, state, level, pos);
                this.rCol = 0.6f * (color >> 16 & 0xFF) / 255f;
                this.gCol = 0.6f * (color >> 8 & 0xFF) / 255f;
                this.bCol = 0.6f * (color & 0xFF) / 255f;
            }
        }
    }
}
