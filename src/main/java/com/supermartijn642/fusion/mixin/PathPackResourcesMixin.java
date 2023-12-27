package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(PathPackResources.class)
public class PathPackResourcesMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private static Logger LOGGER;

    @Final
    @Shadow
    private Path root;
    @Unique
    private Path overridesFolderRoot;

    @Override
    public void setFusionOverridesFolder(@NotNull String folder){
        this.overridesFolderRoot = this.root.resolve(folder);
    }

    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void init(String name, Path root, boolean isBuiltin, CallbackInfo ci){
        Path path = root.resolve("pack.mcmeta");
        if(Files.exists(path)){
            String overridesFolder;
            try(InputStream stream = Files.newInputStream(path)){
                overridesFolder = AbstractPackResources.getMetadataFromStream(FusionPackMetadataSection.INSTANCE, stream);
            }catch(IOException ignored){
                return;
            }
            if(overridesFolder != null)
                this.setFusionOverridesFolder(overridesFolder);
        }
    }

    @Inject(
        method = "getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/IoSupplier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getResource(PackType type, ResourceLocation location, CallbackInfoReturnable<IoSupplier<InputStream>> ci){
        if(this.overridesFolderRoot == null)
            return;

        // Check if the overrides folder contains the requested file
        Path namespaceFolder = this.overridesFolderRoot.resolve(type.getDirectory()).resolve(location.getNamespace());
        IoSupplier<InputStream> supplier = FileUtil.decomposePath(location.getPath()).get().map(list -> {
            Path path = FileUtil.resolvePath(namespaceFolder, list);
            return PathPackResources.returnFileIfExists(path);
        }, o -> null);
        if(supplier != null)
            ci.setReturnValue(supplier);
    }

    @Inject(
        method = "getNamespaces",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> ci){
        if(this.overridesFolderRoot == null)
            return;

        // Add namespaces from the overrides folder
        HashSet<String> namespaces = Sets.newHashSet(ci.getReturnValue());
        Path typeFolder = this.overridesFolderRoot.resolve(type.getDirectory());
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(typeFolder)){
            for(Path directory : stream){
                String location = directory.getFileName().toString();
                if(location.equals(location.toLowerCase(Locale.ROOT))){
                    namespaces.add(location);
                    continue;
                }
                LOGGER.warn("Ignored non-lowercase namespace: {} in {}", location, this.root);
            }
        }catch(NoSuchFileException ignored){
        }catch(IOException e){
            LOGGER.error("Failed to list path {}", typeFolder, e);
        }
        ci.setReturnValue(namespaces);
    }

    @ModifyVariable(
        method = "listResources",
        at = @At("HEAD"),
        ordinal = 0
    )
    private PackResources.ResourceOutput modifyListResources(PackResources.ResourceOutput output, PackType type, String namespace, String path){
        if(this.overridesFolderRoot == null)
            return output;

        // First send all override folder entries, then ignore regular entries which were overridden
        Set<ResourceLocation> overriddenLocations = new HashSet<>();
        FileUtil.decomposePath(path).get().ifLeft(list -> {
            Path namespaceFolder = this.overridesFolderRoot.resolve(type.getDirectory()).resolve(namespace);
            PathPackResources.listPath(namespace, namespaceFolder, list, (location, streamSupplier) -> {
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
