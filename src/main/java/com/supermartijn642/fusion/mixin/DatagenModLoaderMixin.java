package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.predicate.PredicateRegistryImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraftforge.fml.ModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = ModLoader.class, remap = false)
public class DatagenModLoaderMixin {

    @Inject(
        method = "runDataGenerator(Ljava/util/Set;Ljava/nio/file/Path;Ljava/util/Collection;Ljava/util/Collection;ZZZZZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fml/event/lifecycle/GatherDataEvent$DataGeneratorConfig;runAll()V",
            shift = At.Shift.BEFORE
        )
    )
    private void begin(CallbackInfo ci){
        TextureTypeRegistryImpl.finalizeRegistration();
        ModelTypeRegistryImpl.finalizeRegistration();
        PredicateRegistryImpl.finalizeRegistration();
    }
}
