package com.supermartijn642.fusion.mixin.modernfix;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.extensions.ResourceMetadataExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Created 03/01/2025 by SuperMartijn642
 */
@Mixin(targets = "net/minecraft/server/packs/resources/ResourceMetadata$2")
public class ResourceMetadataMixin implements ResourceMetadataExtension {

    /*
     * For some reason the mixin processor just gives up when putting a shadow field in a mixin targeting 'net/minecraft/server/packs/resources/ResourceMetadata$2', shrug
     */

    @Unique
    private JsonObject metadata;
    @Unique
    private boolean intervene = true;

    @Override
    public void disableFusionOverwrite(){
        this.intervene = false;
    }

    @ModifyVariable(
        method = "<init>",
        at = @At("TAIL")
    )
    private JsonObject init(JsonObject json){
        this.metadata = json;
        return json;
    }

    @Inject(
        method = "getSection",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSection(MetadataSectionSerializer<?> serializer, CallbackInfoReturnable<Optional<Object>> ci){
        // If there's Fusion metadata, return null for animation metadata to skip ModernFix's frame size check
        if(this.intervene && serializer == AnimationMetadataSection.SERIALIZER && this.metadata.has(FusionTextureMetadataSection.INSTANCE.getMetadataSectionName()))
            ci.setReturnValue(Optional.empty());
    }
}
