package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraftforge.fmllegacy.packs.ModFileResourcePack;
import net.minecraftforge.forgespi.locating.IModFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created 22/10/2023 by SuperMartijn642
 */
@SuppressWarnings("removal")
@Mixin(value = ModFileResourcePack.class, remap = false)
public class ModFileResourcePackMixin {

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void init(IModFile mf, CallbackInfo ci){
        //noinspection DataFlowIssue
        ModFileResourcePack resources = (ModFileResourcePack)(Object)this;
        if(resources instanceof PackResourcesExtension){
            Path path = mf.findResource("pack.mcmeta");
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
