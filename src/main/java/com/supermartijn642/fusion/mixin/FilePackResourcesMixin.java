package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.FilePack;
import net.minecraft.resources.ResourcePackType;
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
@Mixin(FilePack.class)
public class FilePackResourcesMixin implements PackResourcesExtension {

    @Unique
    private String overridesFolder;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
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
    private void getNamespaces(ResourcePackType type, CallbackInfoReturnable<Set<String>> ci){
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
            if(!name.startsWith(type.getDirectory() + "/") || (list = Lists.newArrayList(FilePack.SPLITTER.split(name))).size() <= 1)
                continue;
            String namespace = list.get(1);
            if(namespace.equals(namespace.toLowerCase(Locale.ROOT))){
                namespaces.add(namespace);
                continue;
            }
            FilePack.LOGGER.warn("Ignored non-lowercase namespace: {} in {}", namespace, ((FilePack)(Object)this).file);
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
        ZipFile zipFile;
        try{
            zipFile = this.getOrCreateZipFile();
        }catch(Exception ignored){
            return;
        }
        if(zipFile == null)
            return;
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        String typeDirectory = this.overridesFolder + type.getDirectory() + "/";
        while(enumeration.hasMoreElements()){
            String name;
            ZipEntry zipEntry = enumeration.nextElement();
            if(zipEntry.isDirectory() || (name = zipEntry.getName()).endsWith(".mcmeta") || !name.startsWith(typeDirectory))
                continue;
            String path = name.substring(typeDirectory.length());
            int slashIndex = path.indexOf('/');
            if(slashIndex >= 0){
                String identifier = path.substring(slashIndex + 1);
                if(!identifier.startsWith(folderName))
                    continue;
                String namespace = path.substring(0, slashIndex);
                ResourceLocation location;
                try{
                    location = new ResourceLocation(namespace, path);
                }catch(Exception ignored){
                    continue;
                }
                if(locationSet.contains(location))
                    continue;
                String[] identifierParts = path.substring(folderName.length() + 2).split("/");
                if(identifierParts.length > depth && predicate.test(identifierParts[identifierParts.length - 1]))
                    locations.add(location);
            }
        }
        ci.setReturnValue(locations);
    }
}
