package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
@Mixin(value = FMLClientHandler.class, remap = false)
public class FMLClientHandlerMixin {

    @Unique
    private static final MetadataSerializer METADATA_SERIALIZER = new MetadataSerializer();

    static{
        METADATA_SERIALIZER.registerMetadataSectionType(FusionPackMetadataSection.INSTANCE, FusionPackMetadataSection.Data.class);
    }

    @Inject(
        method = "addModAsResource",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            shift = At.Shift.AFTER
        )
    )
    private void addModAsResource(ModContainer container, CallbackInfo ci){
        //noinspection DataFlowIssue
        IResourcePack resources = ((FMLClientHandler)(Object)this).getResourcePackFor(container.getModId());
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
