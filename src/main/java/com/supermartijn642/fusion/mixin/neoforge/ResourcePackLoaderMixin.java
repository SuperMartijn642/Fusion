package com.supermartijn642.fusion.mixin.neoforge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.server.packs.PackResources;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 22/06/2026 by SuperMartijn642
 */
@Mixin(ResourcePackLoader.class)
public class ResourcePackLoaderMixin {

    @ModifyExpressionValue(
        method = "readMeta",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;orElse(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 1
        )
    )
    private static Object addFusionOverrideOverlay(Object arg, @Local PackResources resources){
        //noinspection unchecked
        List<String> overlays = (List<String>)arg;
        try{
            FusionPackMetadata metadataSection = resources.getMetadataSection(FusionPackMetadataSection.INSTANCE);
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
