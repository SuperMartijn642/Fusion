package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FilePackResources.class)
public class FilePackResourcesMixin implements PackResourcesExtension {

    @Unique
    private String overridesFolder;

    @Override
    public void setFusionOverridesFolder(String folder){
        this.overridesFolder = folder;
    }

    @Shadow
    private ZipFile getOrCreateZipFile(){
        throw new AssertionError();
    }

    @Inject(
        method = "getResource(Ljava/lang/String;)Ljava/io/InputStream;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getResource(String path, CallbackInfoReturnable<InputStream> ci) throws IOException{
        if(this.overridesFolder == null)
            return;

        // Check if the overrides folder contains the requested file
        path = this.overridesFolder + path;
        ZipFile zipFile = this.getOrCreateZipFile();
        if(zipFile != null){
            ZipEntry zipEntry = zipFile.getEntry(path);
            if(zipEntry != null)
                ci.setReturnValue(zipFile.getInputStream(zipEntry));
        }
    }

    @Inject(
        method = "hasResource(Ljava/lang/String;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hasResource(String path, CallbackInfoReturnable<Boolean> ci){
        if(this.overridesFolder == null)
            return;

        // Check if the overrides folder contains the requested file
        path = this.overridesFolder + path;
        try{
            ZipFile zipFile = this.getOrCreateZipFile();
            if(zipFile != null && zipFile.getEntry(path) != null)
                ci.setReturnValue(true);
        }catch(Exception ignored){
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
        ZipFile zipFile;
        try{
            zipFile = this.getOrCreateZipFile();
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
            if(!name.startsWith(type.getDirectory() + "/") || (list = Lists.newArrayList(FilePackResources.SPLITTER.split(name))).size() <= 1)
                continue;
            String namespace = list.get(1);
            if(namespace.equals(namespace.toLowerCase(Locale.ROOT))){
                namespaces.add(namespace);
                continue;
            }
            FilePackResources.LOGGER.warn("Ignored non-lowercase namespace: {} in {}", namespace, ((FilePackResources)(Object)this).file);
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
        Set<ResourceLocation> locationSet = new HashSet<>(locations);
        // Add all the resources in the overrides folder
        ZipFile zipFile;
        try{
            zipFile = this.getOrCreateZipFile();
        }catch(Exception ignored){
            return;
        }
        if(zipFile == null)
            return;
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        String namespaceDirectory = this.overridesFolder + type.getDirectory() + "/" + namespace + "/";
        String pathDirectory = namespaceDirectory + folderName + "/";
        while(enumeration.hasMoreElements()){
            String name;
            ZipEntry zipEntry = enumeration.nextElement();
            if(zipEntry.isDirectory() || (name = zipEntry.getName()).endsWith(".mcmeta") || !name.startsWith(pathDirectory))
                continue;
            String identifier = name.substring(namespaceDirectory.length());
            ResourceLocation location;
            try{
                location = new ResourceLocation(namespace, identifier);
            }catch(Exception ignored){
                continue;
            }
            if(locationSet.contains(location))
                continue;
            String[] identifierParts = identifier.split("/");
            if(identifierParts.length > depth && predicate.test(identifierParts[identifierParts.length - 1]))
                locations.add(location);
        }
        ci.setReturnValue(locations);
    }
}
