package com.supermartijn642.fusion.mixin;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.resource.PathResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(value = PathResourcePack.class)
public class ForgePathPackResourcesMixin implements PackResourcesExtension {

    @Final
    @Shadow(remap = false)
    private Path source;
    @Unique
    private String overridesFolderName;

    @Override
    public void setFusionOverridesFolder(@Nonnull String folder){
        this.overridesFolderName = folder.replaceAll("/$", "");
    }

    @Shadow(remap = false)
    private Path resolve(String... paths){
        throw new AssertionError();
    }

    @Inject(
        method = "getResource(Ljava/lang/String;)Ljava/io/InputStream;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getResource(String path, CallbackInfoReturnable<InputStream> ci) throws IOException{
        if(this.overridesFolderName == null)
            return;

        Path resolvedPath = this.resolve(this.overridesFolderName, path);
        if(Files.exists(resolvedPath))
            ci.setReturnValue(Files.newInputStream(resolvedPath, StandardOpenOption.READ));
    }

    @Inject(
        method = "hasResource(Ljava/lang/String;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hasResource(String path, CallbackInfoReturnable<Boolean> ci){
        if(this.overridesFolderName == null)
            return;

        if(Files.exists(this.resolve(this.overridesFolderName, path)))
            ci.setReturnValue(true);
    }

    @Inject(
        method = "getNamespaces",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> ci){
        if(this.overridesFolderName == null)
            return;

        // Add namespaces from the overrides folder
        HashSet<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        Path typeFolder = this.resolve(this.overridesFolderName, type.getDirectory());
        try(Stream<Path> walker = Files.walk(typeFolder, 1)){
            walker.filter(Files::isDirectory)
                .map(typeFolder::relativize)
                .filter(p -> p.getNameCount() > 0) // Skip the root entry
                .map(p -> p.toString().replaceAll("/$", "")) // Remove the trailing slash, if present
                .filter(s -> !s.isEmpty()) // Filter empty strings, otherwise empty strings default to minecraft namespace in ResourceLocations
                .forEach(namespaces::add);
        }catch(IOException ignored){
        }
        ci.setReturnValue(namespaces);
    }

    @Inject(
        method = "getResources",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getResources(PackType type, String namespace, String folderName, int depth, Predicate<String> predicate, CallbackInfoReturnable<Collection<ResourceLocation>> ci){
        if(this.overridesFolderName == null)
            return;

        if(ci.getReturnValue() == null)
            return;
        List<ResourceLocation> locations = new ArrayList<>(ci.getReturnValue());

        // Put all locations into a set, so we can look them up quickly
        Set<String> names = locations.stream()
            .map(ResourceLocation::getPath)
            .map(s -> s.startsWith(folderName) ? s.substring(folderName.length()) : s)
            .collect(Collectors.toSet());
        // Add all the resources in the overrides folder
        Path namespaceFolder = this.resolve(this.overridesFolderName, type.getDirectory(), namespace).toAbsolutePath();
        Path searchFolder = namespaceFolder.getFileSystem().getPath(folderName);
        try(Stream<Path> walker = Files.walk(namespaceFolder)){
            walker.map(namespaceFolder::relativize)
                .filter(path -> path.getNameCount() <= depth && !path.toString().endsWith(".mcmeta") && path.startsWith(searchFolder))
                .filter(path -> !names.contains(path.getFileName().toString()))
                .filter(path -> predicate.test(path.getFileName().toString()))
                .map(Joiner.on('/')::join)
                .filter(s -> s.matches("[a-z0-9_/.-]+")) // Only process valid paths Fixes the case where people put invalid resources in their jar.
                .map(path -> new ResourceLocation(namespace, path))
                .forEach(locations::add);
        }catch(IOException ignored){
        }
        ci.setReturnValue(locations);
    }
}
