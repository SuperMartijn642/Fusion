package com.supermartijn642.fusion.mixin.modernfix;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.extensions.ResourceMetadataExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.resources.SimpleResource;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 03/01/2025 by SuperMartijn642
 */
@Mixin(SimpleResource.class)
public class SimpleResourceMixin implements ResourceMetadataExtension {

    @Shadow
    private JsonObject metadata;
    @Unique
    private boolean intervene = true;

    @Override
    public void disableFusionOverwrite(){
        this.intervene = false;
    }

    @Inject(
        method = "getMetadata",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/data/IMetadataSectionSerializer;getMetadataSectionName()Ljava/lang/String;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void getMetadata(IMetadataSectionSerializer<?> serializer, CallbackInfoReturnable<Object> ci){
        // If there's Fusion metadata, return null for animation metadata to skip ModernFix's frame size check
        if(this.intervene && serializer == AnimationMetadataSection.SERIALIZER && this.metadata.has(FusionTextureMetadataSection.INSTANCE.getMetadataSectionName()))
            ci.setReturnValue(null);
    }
}
