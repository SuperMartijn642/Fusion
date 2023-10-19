package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FolderResourcePack.class)
public class FolderResourcePackMixin implements PackResourcesExtension {

    @Unique
    private File overridesFolder;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        //noinspection DataFlowIssue
        this.overridesFolder = new File(((FolderResourcePack)(Object)this).resourcePackFile, folder);
    }

    @Shadow
    private static boolean validatePath(File file, String path) throws IOException{
        throw new AssertionError();
    }

    @Inject(
        method = "getFile",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getFile(String path, CallbackInfoReturnable<File> ci){
        if(this.overridesFolder == null)
            return;

        try{
            File file = new File(this.overridesFolder, path);
            //noinspection ConstantValue
            if(file.isFile() && validatePath(file, path))
                ci.setReturnValue(file);
        }catch(IOException ignored){
        }
    }

    @Inject(
        method = "getResourceDomains",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getResourceDomains(CallbackInfoReturnable<Set<String>> ci){
        if(this.overridesFolder == null)
            return;

        // Add namespaces from the overrides folder
        HashSet<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        File typeFolder = new File(this.overridesFolder, "assets");
        if(typeFolder.isDirectory()){
            File[] folders = typeFolder.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
            if(folders == null)
                return;
            for(File folder : folders){
                String relativePath = AbstractResourcePack.getRelativeName(typeFolder, folder);
                if(relativePath.equals(relativePath.toLowerCase(Locale.ROOT))){
                    namespaces.add(relativePath.substring(0, relativePath.length() - 1));
                    continue;
                }
                FolderResourcePack.LOGGER.warn("Ignored non-lowercase namespace: {} in {}", relativePath, ((FolderResourcePack)(Object)this).resourcePackFile);
            }
        }
        ci.setReturnValue(namespaces);
    }
}
