package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.ResourcePackExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FolderResourcePack.class)
public abstract class FolderResourcePackMixin extends AbstractResourcePack implements ResourcePackExtension {

    @Unique
    private File overridesFolder;

    public FolderResourcePackMixin(File file){
        super(file);
    }

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        this.overridesFolder = new File(this.resourcePackFile, folder);
    }

    @Override
    public Collection<ResourceLocation> fusionGetResources(String folder, int maxDepth, Predicate<String> filter){
        File assetsFolder = new File(this.resourcePackFile, "assets");
        File overwritesAssetsFolder = new File(this.overridesFolder, "assets");
        Set<ResourceLocation> resources = new HashSet<>();
        for(String namespace : this.getResourceDomains()){
            this.listResources(new File(new File(assetsFolder, namespace), folder), maxDepth, namespace, resources, folder + "/", filter);
            this.listResources(new File(new File(overwritesAssetsFolder, namespace), folder), maxDepth, namespace, resources, folder + "/", filter);
        }
        return resources;
    }

    @Unique
    private void listResources(File currentFolder, int maxDepth, String namespace, Collection<ResourceLocation> resources, String folder, Predicate<String> filter){
        File[] files = currentFolder.listFiles();
        if(files != null){
            for(File file : files){
                String fileName = file.getName();
                if(file.isDirectory()){
                    if(maxDepth > 0)
                        this.listResources(file, maxDepth - 1, namespace, resources, folder + fileName + "/", filter);
                }else if(!fileName.endsWith(".mcmeta") && filter.test(fileName) && IdentifierUtil.isValidPath(fileName))
                    resources.add(new ResourceLocation(namespace, folder + fileName));
            }
        }
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
