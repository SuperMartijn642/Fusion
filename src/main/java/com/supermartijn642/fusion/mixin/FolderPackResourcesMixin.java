package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackType;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FolderPackResources.class)
public class FolderPackResourcesMixin implements PackResourcesExtension {

    @Unique
    private File overridesFolder;

    @Override
    public void setFusionOverridesFolder(String folder){
        //noinspection DataFlowIssue
        this.overridesFolder = new File(((FolderPackResources)(Object)this).file, folder);
    }

    @Shadow
    private void listResources(File folder, int depth, String namespace, List<ResourceLocation> locations, String folderPath, Predicate<String> namePredicate){
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
            if(file.isFile() && FolderPackResources.validatePath(file, path))
                ci.setReturnValue(file);
        }catch(IOException ignored){
        }
    }

    @Inject(
        method = "getNamespaces",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> ci){
        if(this.overridesFolder == null)
            return;

        // Add namespaces from the overrides folder
        HashSet<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        File typeFolder = new File(this.overridesFolder, type.getDirectory());
        File[] folders = typeFolder.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if(folders == null)
            return;
        for(File folder : folders){
            String relativePath = FolderPackResources.getRelativePath(typeFolder, folder);
            if(relativePath.equals(relativePath.toLowerCase(Locale.ROOT))){
                namespaces.add(relativePath.substring(0, relativePath.length() - 1));
                continue;
            }
            //noinspection DataFlowIssue
            ((FolderPackResources)(Object)this).logWarning(relativePath);
        }
        ci.setReturnValue(namespaces);
    }

    @Inject(
        method = "getResources",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getResources(PackType type, String namespace, String folderName, int depth, Predicate<String> predicate, CallbackInfoReturnable<Collection<ResourceLocation>> ci){
        if(this.overridesFolder == null)
            return;

        if(ci.getReturnValue() == null)
            return;
        List<ResourceLocation> locations = ci.getReturnValue() instanceof ArrayList<ResourceLocation> ?
            ((List<ResourceLocation>)ci.getReturnValue()) :
            new ArrayList<>(ci.getReturnValue());

        // Put all locations into a set, so we can look them up quickly
        Set<String> names = locations.stream()
            .map(ResourceLocation::getPath)
            .map(s -> s.startsWith(folderName) ? s.substring(folderName.length()) : s)
            .collect(Collectors.toSet());
        // Add all the resources in the overrides folder
        File folder = new File(new File(new File(this.overridesFolder, type.getDirectory()), namespace), folderName);
        this.listResources(folder, depth, namespace, locations, folderName, name -> !names.contains(name) && predicate.test(name));
        ci.setReturnValue(locations);
    }
}
