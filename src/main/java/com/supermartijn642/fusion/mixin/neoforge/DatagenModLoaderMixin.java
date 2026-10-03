package com.supermartijn642.fusion.mixin.neoforge;

import com.supermartijn642.fusion.FusionClient;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = DatagenModLoader.class, remap = false)
public class DatagenModLoaderMixin {

    /**
     * NeoForge 26.3.0.36 moved {@code GatherDataEvent.DataGeneratorConfig} to its own top level class,
     * {@code DataGeneratorConfig}. The jar runs on builds from before and after that change, so both
     * call targets are listed and exactly one of them must match.
     */
    @Group(name = "runAll", min = 1, max = 1)
    @Inject(
        method = "begin",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/data/event/GatherDataEvent$DataGeneratorConfig;runAll()V",
            shift = At.Shift.BEFORE
        ),
        require = 0
    )
    private static void begin(CallbackInfo ci){
        FusionClient.finalizeRegistries();
    }

    @Group(name = "runAll", min = 1, max = 1)
    @Inject(
        method = "begin",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/data/event/DataGeneratorConfig;runAll()V",
            shift = At.Shift.BEFORE
        ),
        require = 0
    )
    private static void beginDataGeneratorConfig(CallbackInfo ci){
        FusionClient.finalizeRegistries();
    }
}
