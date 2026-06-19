package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(Pack.class)
public class PackMixin implements PackExtension {

    @Unique
    private FusionPackMetadata metadata;

    @Override
    public @Nullable FusionPackMetadata getFusionMetadata(){
        return this.metadata;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/repository/Pack$Metadata;Lnet/minecraft/server/packs/PackSelectionConfig;Ljava/util/List;)V",
        at = @At("RETURN")
    )
    private void init(PackLocationInfo locationInfo, Pack.ResourcesSupplier resourcesSupplier, Pack.Metadata metadata, PackSelectionConfig config, List<Pack> children, CallbackInfo ci){
        try(PackResources resources = resourcesSupplier.openPrimary(locationInfo)){
            this.metadata = resources.getMetadataSection(FusionPackMetadataSection.INSTANCE);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst reading fusion metadata for pack '" + locationInfo.id() + "':", e);
        }
    }

    @ModifyArg(
        method = "readPackMetadata",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/Pack$Metadata;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/server/packs/repository/PackCompatibility;Lnet/minecraft/world/flag/FeatureFlagSet;Ljava/util/List;Z)V"
        ),
        index = 3
    )
    private static List<String> addFusionOverrideOverlay(List<String> overlays, @Local PackResources resources) {
        try{
            FusionPackMetadata metadataSection = resources.getMetadataSection(FusionPackMetadataSection.INSTANCE);
            if (metadataSection != null && metadataSection.hasOverridesFolder()) {
                overlays = new LinkedList<>(overlays);
                String overridesFolder = metadataSection.getOverridesFolder();
                // trim trailing slash
                overridesFolder = overridesFolder.substring(0, overridesFolder.length() - 1);
                // ensure we are first (overlays will be reversed in CompositePackResources constructor) to override all other assets
                overlays.addLast(overridesFolder);
            }
        }catch(Exception ignored){
        }
        return overlays;
    }
}
