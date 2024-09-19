package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.overlays.BlockModelOverlayReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
        method = "init",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;modelManager:Lnet/minecraft/client/renderer/block/model/ModelManager;",
            shift = At.Shift.BEFORE,
            ordinal = 1
        )
    )
    private void registerBlockModelOverlayReloadListener(CallbackInfo ci){
        SimpleReloadableResourceManager resourceManager = (SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
        resourceManager.registerReloadListener(BlockModelOverlayReloadListener.INSTANCE);
    }
}
