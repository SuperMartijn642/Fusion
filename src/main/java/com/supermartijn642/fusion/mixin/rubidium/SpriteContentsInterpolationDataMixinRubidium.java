package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.texture.FusionSpriteContents;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 30/06/2026 by SuperMartijn642
 */
@Mixin(value = SpriteContents.InterpolationData.class, priority = 1100)
public class SpriteContentsInterpolationDataMixinRubidium {

    /*
     * Sodium overwrites InterpolationData#uploadInterpolatedFrame, bypassing the use of #getPixel.
     * This breaks Fusion's animations as we override #getPixel in our InterpolationData class to use the correct image coordinates.
     * Due to https://github.com/neoforged/NeoForge/issues/3270, we cannot override #uploadInterpolatedFrame in our InterpolationData class.
     * Hence, the only option is to mixin into Sodium's overwrite.
     */

    @Inject(
        method = "uploadInterpolatedFrame",
        at = @At("HEAD"),
        cancellable = true
    )
    private void uploadInterpolatedFrame(int atlasX, int atlasY, SpriteContents.Ticker ticker, CallbackInfo ci) {
        if(this instanceof FusionSpriteContents.SodiumBypassingInterpolationData){
            ((FusionSpriteContents.SodiumBypassingInterpolationData)this).bypassSodiumUploadInterpolatedFrameOverwrite(atlasX, atlasY, ticker);
            ci.cancel();
        }
    }
}
