package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(ModNioResourcePack.class)
public class ModNioResourcePackMixin {

    @Shadow
    private static boolean exists(Path path){
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "create",
        at = @At("STORE"),
        ordinal = 1
    )
    private static List<Path> create(List<Path> rootPaths, String id, ModContainer mod, String subPath, PackType type){
        List<Path> newRootPaths = null;
        for(Path rootPath : rootPaths){
            Path metaPath = rootPath.resolve("pack.mcmeta").toAbsolutePath().normalize();
            if(!metaPath.startsWith(rootPath) || !exists(metaPath))
                continue;

            Path overridesPath = null;
            try(InputStream stream = Files.newInputStream(metaPath)){
                String overridesFolder = AbstractPackResources.getMetadataFromStream(FusionPackMetadataSection.INSTANCE, stream);
                if(overridesFolder != null)
                    overridesPath = rootPath.resolve(overridesFolder);
            }catch(Exception ignore){
                continue;
            }

            if(overridesPath != null){
                if(newRootPaths == null)
                    newRootPaths = new ArrayList<>(rootPaths);
                newRootPaths.add(overridesPath);
            }
        }
        return newRootPaths == null ? rootPaths : newRootPaths;
    }
}
