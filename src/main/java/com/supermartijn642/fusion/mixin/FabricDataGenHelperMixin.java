package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.predicate.PredicateRegistryImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 03/05/2023 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(FabricDataGenHelper.class)
public class FabricDataGenHelperMixin {

    @Inject(
        method = "run()V",
        at = @At("HEAD"),
        remap = false
    )
    private static void run(CallbackInfo ci){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT){
            TextureTypeRegistryImpl.finalizeRegistration();
            ModelTypeRegistryImpl.finalizeRegistration();
            PredicateRegistryImpl.finalizeRegistration();
        }
    }
}
