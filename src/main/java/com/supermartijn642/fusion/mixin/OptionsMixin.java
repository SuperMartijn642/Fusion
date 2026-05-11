package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 07/05/2026 by SuperMartijn642
 */
@Mixin(Options.class)
public class OptionsMixin {

    @Inject(
        method = "loadSelectedResourcePacks",
        at = @At("HEAD")
    )
    private void finalizeRegistries(CallbackInfo ci){
        // We want to mixin into Minecraft#<init>, but still Forge's version of Mixin does not allow for mixins into constructors.
        // So this dumb shit is needed
        FusionClient.finalizeRegistries();
    }
}
