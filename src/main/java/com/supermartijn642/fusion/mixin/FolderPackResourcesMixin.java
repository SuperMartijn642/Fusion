package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
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
import java.util.*;
import java.util.function.Predicate;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FolderPack.class)
public class FolderPackResourcesMixin implements PackResourcesExtension {

    @Unique
    private File overridesFolder;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        //noinspection DataFlowIssue
        this.overridesFolder = new File(((FolderPack)(Object)this).file, folder);
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
            if(file.isFile() && FolderPack.validatePath(file, path))
                ci.setReturnValue(file);
        }catch(IOException ignored){
        }
    }

    @Inject(
        method = "getNamespaces",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getNamespaces(ResourcePackType type, CallbackInfoReturnable<Set<String>> ci){
        if(this.overridesFolder == null)
            return;

        // Add namespaces from the overrides folder
        HashSet<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        File typeFolder = new File(this.overridesFolder, type.getDirectory());
        File[] folders = typeFolder.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
        if(folders == null)
            return;
        for(File folder : folders){
            String relativePath = FolderPack.getRelativePath(typeFolder, folder);
            if(relativePath.equals(relativePath.toLowerCase(Locale.ROOT))){
                namespaces.add(relativePath.substring(0, relativePath.length() - 1));
                continue;
            }
            //noinspection DataFlowIssue
            ((FolderPack)(Object)this).logWarning(relativePath);
        }
        ci.setReturnValue(namespaces);
    }

    @Inject(
        method = "getResources",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getResources(ResourcePackType type, String folderName, int depth, Predicate<String> predicate, CallbackInfoReturnable<Collection<ResourceLocation>> ci){
        if(this.overridesFolder == null)
            return;

        if(ci.getReturnValue() == null)
            return;
        List<ResourceLocation> locations = ci.getReturnValue() instanceof ArrayList ?
            ((List<ResourceLocation>)ci.getReturnValue()) :
            new ArrayList<>(ci.getReturnValue());

        // Put all locations into a set, so we can look them up quickly
        Set<ResourceLocation> locationSet = new HashSet<>(locations);
        // Add all the resources in the overrides folder
        List<ResourceLocation> newLocations = new ArrayList<>();
        File typeFolder = new File(this.overridesFolder, type.getDirectory());
        //noinspection DataFlowIssue
        for(String namespace : ((FolderPack)(Object)this).getNamespaces(type)){
            File folder = new File(new File(typeFolder, namespace), folderName);
            this.listResources(folder, depth, namespace, newLocations, folderName + "/", predicate);
        }
        newLocations.stream()
            .filter(l -> !locationSet.contains(l))
            .forEach(locations::add);
        ci.setReturnValue(locations);
    }
}
