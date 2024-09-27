package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.EntityExtension;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 27/09/2024 by SuperMartijn642
 */
@Mixin(AgeableMob.class)
public class AgeableMobMixin {

    @Inject(
        method = "setAge",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/AgeableMob;ageBoundaryReached()V"
        )
    )
    private void setAge(CallbackInfo ci){
        ((EntityExtension)this).markFusionRecomputeModels();
    }
}
