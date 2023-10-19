package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(Pack.class)
public class PackMixin {

    @Unique
    private String overridesFolder;

    @Inject(
        method = "<init>(Ljava/lang/String;ZLjava/util/function/Supplier;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/server/packs/repository/PackCompatibility;Lnet/minecraft/server/packs/repository/Pack$Position;ZLnet/minecraft/server/packs/repository/PackSource;Z)V",
        at = @At("RETURN")
    )
    private void init(String identifier, boolean required, Supplier<PackResources> resourcesSupplier, Component title, Component description, PackCompatibility compatibility, Pack.Position position, boolean fixedPosition, PackSource packSource, boolean hidden, CallbackInfo ci){
        try(PackResources resources = resourcesSupplier.get()){
            this.overridesFolder = resources.getMetadataSection(FusionPackMetadataSection.INSTANCE);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst reading fusion metadata for pack '" + identifier + "':", e);
        }
    }

    @Inject(
        method = "open",
        at = @At("RETURN")
    )
    private void open(CallbackInfoReturnable<PackResources> ci){
        PackResources resources = ci.getReturnValue();
        if(this.overridesFolder != null && resources instanceof PackResourcesExtension)
            ((PackResourcesExtension)resources).setFusionOverridesFolder(this.overridesFolder);
    }
}
