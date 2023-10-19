package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FilePackResources.class)
public class FilePackResourcesMixin implements PackResourcesExtension {

    @Unique
    private String overridesFolder;

    @Final
    @Shadow
    private File file;

    @Override
    public void setFusionOverridesFolder(String folder){
        this.overridesFolder = folder;
    }

    @Shadow
    private ZipFile getOrCreateZipFile(){
        throw new AssertionError();
    }

    @Inject(
        method = "getResource(Ljava/lang/String;)Lnet/minecraft/server/packs/resources/IoSupplier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getResource(String path, CallbackInfoReturnable<IoSupplier<InputStream>> ci){
        if(this.overridesFolder == null)
            return;

        // Check if the overrides folder contains the requested file
        path = this.overridesFolder + path;
        ZipFile zipFile = this.getOrCreateZipFile();
        if(zipFile != null){
            ZipEntry zipEntry = zipFile.getEntry(path);
            if(zipEntry != null)
                ci.setReturnValue(IoSupplier.create(zipFile, zipEntry));
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
        ZipFile zipFile = this.getOrCreateZipFile();
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
            if(!name.startsWith(type.getDirectory() + "/") || (list = Lists.newArrayList(FilePackResources.SPLITTER.split(name))).size() <= 1)
                continue;
            String namespace = list.get(1);
            if(namespace.equals(namespace.toLowerCase(Locale.ROOT))){
                namespaces.add(namespace);
                continue;
            }
            FilePackResources.LOGGER.warn("Ignored non-lowercase namespace: {} in {}", namespace, this.file);
        }
        ci.setReturnValue(namespaces);
    }

    @ModifyVariable(
        method = "listResources",
        at = @At("HEAD"),
        ordinal = 0
    )
    private PackResources.ResourceOutput modifyListResources(PackResources.ResourceOutput output, PackType type, String namespace, String path){
        if(this.overridesFolder == null)
            return output;

        // First send all override folder entries, then ignore regular entries which were overridden
        ZipFile zipFile = this.getOrCreateZipFile();
        if(zipFile == null)
            return output;
        Set<ResourceLocation> overriddenLocations = new HashSet<>();
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        String namespaceDirectory = this.overridesFolder + type.getDirectory() + "/" + namespace + "/";
        String pathDirectory = namespaceDirectory + path + "/";
        while(enumeration.hasMoreElements()){
            String name;
            ZipEntry zipEntry = enumeration.nextElement();
            if(zipEntry.isDirectory() || !(name = zipEntry.getName()).startsWith(pathDirectory)) continue;
            String identifier = name.substring(namespaceDirectory.length());
            ResourceLocation location = ResourceLocation.tryBuild(namespace, identifier);
            if(location != null){
                overriddenLocations.add(location);
                output.accept(location, IoSupplier.create(zipFile, zipEntry));
                continue;
            }
            FilePackResources.LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", namespace, identifier);
        }

        // Filter all output resources
        return (location, streamSupplier) -> {
            if(!overriddenLocations.contains(location))
                output.accept(location, streamSupplier);
        };
    }
}
