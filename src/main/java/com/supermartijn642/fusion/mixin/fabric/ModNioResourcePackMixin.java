package com.supermartijn642.fusion.mixin.fabric;

import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.minecraft.server.packs.AbstractPackResources;
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

    @Shadow(remap = false)
    private static boolean exists(Path path){
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "create",
        at = @At("STORE"),
        ordinal = 0
    )
    private static List<Path> create(List<Path> rootPaths){
        List<Path> newRootPaths = null;
        for(Path rootPath : rootPaths){
            Path metaPath = rootPath.resolve("pack.mcmeta").toAbsolutePath().normalize();
            if(!exists(metaPath))
                continue;

            Path overridesPath = null;
            try(InputStream stream = Files.newInputStream(metaPath)){
                FusionPackMetadata metadata = AbstractPackResources.getMetadataFromStream(FusionPackMetadataSection.TYPE, stream);
                if(metadata != null && metadata.hasOverridesFolder())
                    overridesPath = rootPath.resolve(metadata.getOverridesFolder());
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
