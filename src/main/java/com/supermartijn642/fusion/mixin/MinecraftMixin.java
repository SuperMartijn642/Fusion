package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 07/05/2026 by SuperMartijn642
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/System;currentTimeMillis()J",
            ordinal = 0
        )
    )
    private void finalizeRegistries(CallbackInfo ci){
        FusionClient.finalizeRegistries();
    }
}
