package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.FusionClient;
import net.minecraftforge.fmllegacy.DatagenModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = DatagenModLoader.class, remap = false)
public class DatagenModLoaderMixin {

    @Inject(
        method = "begin",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/forge/event/lifecycle/GatherDataEvent$DataGeneratorConfig;runAll()V",
            shift = At.Shift.BEFORE
        )
    )
    private static void begin(CallbackInfo ci){
        FusionClient.finalizeRegistries();
    }
}
