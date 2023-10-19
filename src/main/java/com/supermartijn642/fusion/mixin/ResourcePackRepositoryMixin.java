package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.data.MetadataSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(ResourcePackRepository.class)
public class ResourcePackRepositoryMixin {

    @Unique
    private static final MetadataSerializer METADATA_SERIALIZER = new MetadataSerializer();

    static{
        METADATA_SERIALIZER.registerMetadataSectionType(FusionPackMetadataSection.INSTANCE, FusionPackMetadataSection.Data.class);
    }

    @Inject(
        method = "getResourcePack(Ljava/io/File;)Lnet/minecraft/client/resources/IResourcePack;",
        at = @At("RETURN")
    )
    private void getResourcePack(File file, CallbackInfoReturnable<IResourcePack> ci){
        IResourcePack resources = ci.getReturnValue();
        if(resources instanceof PackResourcesExtension){
            String overridesFolder;
            try{
                FusionPackMetadataSection.Data data = resources.getPackMetadata(METADATA_SERIALIZER, FusionPackMetadataSection.INSTANCE.getSectionName());
                overridesFolder = data == null ? null : data.overridesFolder;
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading fusion metadata for pack '" + resources.getPackName() + "':", e);
                return;
            }
            if(overridesFolder != null)
                ((PackResourcesExtension)resources).setFusionOverridesFolder(overridesFolder);
        }
    }
}
