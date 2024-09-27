package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.BufferSourceExtension;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 02/10/2024 by SuperMartijn642
 */
@Mixin(OutlineBufferSource.class)
public class OutlineBufferSourceMixin implements BufferSourceExtension {

    @Unique
    private RenderType lastRenderType;

    @Override
    public RenderType fusionGetLastRenderType(){
        return this.lastRenderType;
    }

    @Inject(
        method = "getBuffer",
        at = @At("HEAD")
    )
    private void getBuffer(RenderType renderType, CallbackInfoReturnable<?> ci){
        this.lastRenderType = renderType;
    }
}
