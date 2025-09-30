package com.supermartijn642.fusion.mixin;

import net.minecraft.server.packs.metadata.pack.PackFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
@Mixin(PackFormat.IntermediaryFormat.class)
public class IntermediaryFormatMixin {

    @Inject(
        method = "validatePackFormatForRange",
        at = @At("HEAD"),
        cancellable = true
    )
    private void validatePackFormatForRange(int min, int max, CallbackInfoReturnable<String> ci){
        // Without this, it is impossible to support 1.19.4- and 1.21.9+ in one resource pack
        //noinspection DataFlowIssue
        int format = ((PackFormat.IntermediaryFormat)(Object)this).format().get();
        if(format < 15 && format >= min && format <= max)
            ci.setReturnValue(null);
    }
}
