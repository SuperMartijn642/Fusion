package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.resource.PathResourcePack;
import net.minecraftforge.resource.ResourcePackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@Mixin(value = ResourcePackLoader.class, remap = false)
public class ResourcePackLoaderMixin {

    @Inject(
        method = "createPackForMod",
        at = @At("RETURN")
    )
    private static void createPackForMod(IModFileInfo mf, CallbackInfoReturnable<PathResourcePack> ci){
        PathResourcePack resources = ci.getReturnValue();
        if(resources instanceof PackResourcesExtension){
            Path path = mf.getFile().findResource("pack.mcmeta");
            String overridesFolder;
            try(InputStream stream = Files.newInputStream(path)){
                overridesFolder = AbstractPackResources.getMetadataFromStream(FusionPackMetadataSection.INSTANCE, stream);
            }catch(IOException ignored){
                return;
            }
            if(overridesFolder != null)
                ((PackResourcesExtension)resources).setFusionOverridesFolder(overridesFolder);
        }
    }
}
