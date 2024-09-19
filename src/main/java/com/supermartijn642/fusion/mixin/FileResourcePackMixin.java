package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.ResourcePackExtension;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FileResourcePack.class)
public class FileResourcePackMixin implements ResourcePackExtension {

    @Unique
    private String overridesFolder;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        this.overridesFolder = folder;
    }

    @Override
    public Collection<ResourceLocation> fusionGetResources(String folder, int maxDepth, Predicate<String> filter){
        ZipFile zipfile;
        try{
            zipfile = this.getResourcePackZipFile();
        }catch(IOException e){
            return Collections.emptySet();
        }

        Set<ResourceLocation> resources = new HashSet<>();
        String assetsPath = "assets/", overwritesAssetsPath = this.overridesFolder + assetsPath;

        // Go through all files in the zip file
        Enumeration<? extends ZipEntry> entries = zipfile.entries();
        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            if(entry.isDirectory()) // Check entry is within the assets folder
                continue;
            if(entry.getName().endsWith(".mcmeta")) // Ignore metadata files
                continue;
            String path;
            if(entry.getName().startsWith(assetsPath))
                path = entry.getName().substring(assetsPath.length());
            else if(entry.getName().startsWith(overwritesAssetsPath))
                path = entry.getName().substring(overwritesAssetsPath.length());
            else
                continue; // Ignore files not in the assets folder
            int namespaceEnd = path.indexOf('/');
            if(namespaceEnd < 0) // Ignore any files directly in the assets folder
                continue;
            String file = path.substring(namespaceEnd + 1);
            if(file.startsWith(folder + "/")){
                String[] fileParts = file.substring(folder.length() + 2).split("/");
                if(fileParts.length >= maxDepth + 1 && filter.test(file)){
                    String namespace = path.substring(0, namespaceEnd);
                    resources.add(new ResourceLocation(namespace, file));
                }
            }
        }

        return resources;
    }

    @Shadow
    private ZipFile getResourcePackZipFile() throws IOException{
        throw new AssertionError();
    }

    @Inject(
        method = "getInputStreamByName(Ljava/lang/String;)Ljava/io/InputStream;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getInputStreamByName(String path, CallbackInfoReturnable<InputStream> ci) throws IOException{
        if(this.overridesFolder == null)
            return;

        // Check if the overrides folder contains the requested file
        path = this.overridesFolder + path;
        ZipFile zipFile;
        try{
            zipFile = this.getResourcePackZipFile();
        }catch(Exception ignored){
            return;
        }
        if(zipFile != null){
            ZipEntry zipEntry = zipFile.getEntry(path);
            if(zipEntry != null)
                ci.setReturnValue(zipFile.getInputStream(zipEntry));
        }
    }

    @Inject(
        method = "hasResourceName(Ljava/lang/String;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hasResourceName(String path, CallbackInfoReturnable<Boolean> ci){
        if(this.overridesFolder == null)
            return;

        // Check if the overrides folder contains the requested file
        path = this.overridesFolder + path;
        try{
            ZipFile zipFile = this.getResourcePackZipFile();
            if(zipFile != null && zipFile.getEntry(path) != null)
                ci.setReturnValue(true);
        }catch(Exception ignored){
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
        ZipFile zipFile;
        try{
            zipFile = this.getResourcePackZipFile();
        }catch(Exception ignored){
            return;
        }
        if(zipFile == null)
            return;
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        Set<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        while(enumeration.hasMoreElements()){
            ArrayList<String> list;
            ZipEntry zipEntry = enumeration.nextElement();
            String name = zipEntry.getName();
            if(!name.startsWith(this.overridesFolder))
                continue;
            name = name.substring(this.overridesFolder.length());
            if(!name.startsWith("assets/") || (list = Lists.newArrayList(FileResourcePack.ENTRY_NAME_SPLITTER.split(name))).size() <= 1)
                continue;
            String namespace = list.get(1);
            if(namespace.equals(namespace.toLowerCase(Locale.ROOT))){
                namespaces.add(namespace);
                continue;
            }
            FileResourcePack.LOGGER.warn("Ignored non-lowercase namespace: {} in {}", namespace, ((FileResourcePack)(Object)this).resourcePackFile);
        }
        ci.setReturnValue(namespaces);
    }
}
