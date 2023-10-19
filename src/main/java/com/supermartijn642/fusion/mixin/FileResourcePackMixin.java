package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.client.resources.FileResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(FileResourcePack.class)
public class FileResourcePackMixin implements PackResourcesExtension {

    @Unique
    private String overridesFolder;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        this.overridesFolder = folder;
    }

    @Shadow
    private ZipFile getResourcePackZipFile(){
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
