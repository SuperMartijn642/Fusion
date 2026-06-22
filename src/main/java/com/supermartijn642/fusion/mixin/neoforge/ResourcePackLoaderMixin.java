package com.supermartijn642.fusion.mixin.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created 22/06/2026 by SuperMartijn642
 */
@Mixin(ResourcePackLoader.class)
public class ResourcePackLoaderMixin {

    @ModifyArg(
        method = "readMeta",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"
        ),
        index = 0
    )
    private static Collection<String> addFusionOverrideOverlay(Collection<String> overlays, @Local PackResources resources){
        try{
            FusionPackMetadata metadataSection = resources.getMetadataSection(FusionPackMetadataSection.TYPE);
            if(metadataSection != null && metadataSection.hasOverridesFolder()){
                List<String> copy = new ArrayList<>(overlays.size() + 1);
                copy.addAll(overlays);
                overlays = copy;
                String overridesFolder = metadataSection.getOverridesFolder();
                // trim trailing slash
                overridesFolder = overridesFolder.substring(0, overridesFolder.length() - 1);
                // ensure we are first (overlays will be reversed in CompositePackResources constructor) to override all other assets
                overlays.add(overridesFolder);
            }
        }catch(Exception ignored){
        }
        return overlays;
    }
}
