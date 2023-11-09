package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.resource.PathPackResources;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(value = PathPackResources.class)
public class ForgePathPackResourcesMixin implements PackResourcesExtension {

    @Final
    @Shadow(remap = false)
    private static Logger LOGGER;

    @Final
    @Shadow(remap = false)
    private Path source;
    @Unique
    private String overridesFolderName;

    @Override
    public void setFusionOverridesFolder(@NotNull String folder){
        this.overridesFolderName = folder;
    }

    @Shadow(remap = false)
    private static String[] getPathFromLocation(PackType type, ResourceLocation location){
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private Path resolve(String... paths){
        throw new AssertionError();
    }

    @Inject(
        method = "getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/IoSupplier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getResource(PackType type, ResourceLocation location, CallbackInfoReturnable<IoSupplier<InputStream>> ci){
        if(this.overridesFolderName == null)
            return;

        // Check if the overrides folder contains the requested file
        String[] pathParts = getPathFromLocation(location.getPath().startsWith("lang/") ? PackType.CLIENT_RESOURCES : type, location);
        String[] overridePathParts = new String[pathParts.length + 1];
        overridePathParts[0] = this.overridesFolderName;
        System.arraycopy(pathParts, 0, overridePathParts, 1, pathParts.length);
        Path path = this.resolve(overridePathParts);
        if(Files.exists(path))
            ci.setReturnValue(IoSupplier.create(path));
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
        }catch(NoSuchFileException ignored){
        }catch(IOException e){
            LOGGER.error("Failed to walk path {}", typeFolder, e);
        }
        ci.setReturnValue(namespaces);
    }

    @ModifyVariable(
        method = "listResources",
        at = @At("HEAD"),
        ordinal = 0
    )
    private PackResources.ResourceOutput modifyListResources(PackResources.ResourceOutput output, PackType type, String namespace, String path){
        if(this.overridesFolderName == null)
            return output;

        // First send all override folder entries, then ignore regular entries which were overridden
        Set<ResourceLocation> overriddenLocations = new HashSet<>();
        FileUtil.decomposePath(path).get().ifLeft(list -> {
            Path namespaceFolder = this.resolve(this.overridesFolderName, type.getDirectory(), namespace).toAbsolutePath();
            net.minecraft.server.packs.PathPackResources.listPath(namespace, namespaceFolder, list, (location, streamSupplier) -> {
                overriddenLocations.add(location);
                output.accept(location, streamSupplier);
            });
        }).ifRight(partialResult -> LOGGER.error("Invalid path {}: {}", path, partialResult.message()));

        // Filter all output resources
        return (location, streamSupplier) -> {
            if(!overriddenLocations.contains(location))
                output.accept(location, streamSupplier);
        };
    }
}
