package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.items.ItemModelPredicatesReloadListener;
import com.supermartijn642.fusion.model.overlays.BlockModelOverlayReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
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
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelManager;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/color/block/BlockColors;I)V",
            shift = At.Shift.BEFORE
        )
    )
    private void registerBlockModelOverlayReloadListener(CallbackInfo ci){
        //noinspection DataFlowIssue
        ReloadableResourceManager resourceManager = ((ReloadableResourceManager)((Minecraft)(Object)this).getResourceManager());
        resourceManager.registerReloadListener(BlockModelOverlayReloadListener.INSTANCE);
        resourceManager.registerReloadListener(ItemModelPredicatesReloadListener.INSTANCE);
    }
}
