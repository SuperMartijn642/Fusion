package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.extensions.PackResourcesExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.PackCompatibility;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.util.function.Supplier;

/**
 * Created 19/10/2023 by SuperMartijn642
 */
@Mixin(ResourcePackInfo.class)
public class PackMixin implements PackExtension {

    @Unique
    private FusionPackMetadata metadata;

    @Override
    public @Nullable FusionPackMetadata getFusionMetadata(){
        return this.metadata;
    }

    @Inject(
        method = "<init>(Ljava/lang/String;ZLjava/util/function/Supplier;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/resources/PackCompatibility;Lnet/minecraft/resources/ResourcePackInfo$Priority;ZZ)V",
        at = @At("RETURN")
    )
    private void init(String identifier, boolean required, Supplier<IResourcePack> resourcesSupplier, ITextComponent title, ITextComponent description, PackCompatibility compatibility, ResourcePackInfo.Priority position, boolean fixedPosition, boolean hidden, CallbackInfo ci){
        try(IResourcePack resources = resourcesSupplier.get()){
            this.metadata = resources.getMetadataSection(FusionPackMetadataSection.INSTANCE);
        }catch(FileNotFoundException ignore){
            // Ignore resource packs which don't have a pack.mcmeta file
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst reading fusion metadata for pack '" + identifier + "':", e);
        }
    }

    @Inject(
        method = "open",
        at = @At("RETURN")
    )
    private void open(CallbackInfoReturnable<IResourcePack> ci){
        IResourcePack resources = ci.getReturnValue();
        if(this.metadata != null && this.metadata.hasOverridesFolder() && resources instanceof PackResourcesExtension)
            ((PackResourcesExtension)resources).setFusionOverridesFolder(this.metadata.getOverridesFolder());
    }
}
