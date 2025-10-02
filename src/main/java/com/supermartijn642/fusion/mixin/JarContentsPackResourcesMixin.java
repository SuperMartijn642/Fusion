package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import cpw.mods.jarhandling.JarContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.resource.JarContentsPackResources;
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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created 02/10/2025 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(JarContentsPackResources.class)
public class JarContentsPackResourcesMixin implements PackResourcesExtension {

    @Final
    @Shadow
    private static Logger LOGGER;

    @Unique
    private String overridesFolder;

    @Final
    @Shadow
    private JarContents contents;

    @Override
    public void setFusionOverridesFolder(@NotNull String folder){
        this.overridesFolder = folder;
    }

    @Shadow
    private String addPrefix(String string){
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
        var resource = this.contents.get(this.addPrefix(path));
        if(resource != null)
            ci.setReturnValue(resource::open);
    }

    @ModifyReturnValue(
        method = "getNamespaces",
        at = @At("RETURN")
    )
    private Set<String> getNamespaces(Set<String> initialNamespaces, PackType type){
        if(this.overridesFolder == null)
            return initialNamespaces;

        // Add namespaces from the overrides folder
        Set<String> namespaces = Sets.newHashSet(initialNamespaces);
        String prefix = this.addPrefix(this.overridesFolder + type.getDirectory() + "/");
        this.contents.visitContent(prefix, (relativePath, resource) -> {
            if(!relativePath.startsWith(prefix))
                throw new IllegalStateException("Path received from visitContent doesn't start with prefix '" + prefix + "': " + relativePath);

            // Extract the namespace
            int prefixLength = prefix.length();
            int directoryIndex = relativePath.indexOf('/', prefixLength);
            if(directoryIndex == -1)
                return; // Ignore files that are directly beneath the prefix, only directories can be namespaces
            var namespace = relativePath.substring(prefixLength, directoryIndex);
            if(ResourceLocation.isValidNamespace(namespace))
                namespaces.add(namespace);
            else
                LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in Fusion overrides in pack {}, ignoring", namespace, this.contents);
        });
        return namespaces;
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
        Set<ResourceLocation> overriddenLocations = new HashSet<>();
        String namespaceDirectory = this.addPrefix(this.overridesFolder + type.getDirectory() + "/" + namespace + "/");
        String pathDirectory = namespaceDirectory + path + "/";
        this.contents.visitContent(pathDirectory, (relativePath, resource) -> {
            String identifier = relativePath.substring(namespaceDirectory.length());
            ResourceLocation location = ResourceLocation.tryBuild(namespace, identifier);
            if(location != null){
                overriddenLocations.add(location);
                output.accept(location, resource.retain()::open);
            }else
                LOGGER.warn("Invalid path in Fusion overrides in datapack: {}:{}, ignoring", namespace, identifier);
        });

        // Filter all output resources
        return (location, streamSupplier) -> {
            if(!overriddenLocations.contains(location))
                output.accept(location, streamSupplier);
        };
    }
}
