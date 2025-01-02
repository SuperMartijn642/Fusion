package com.supermartijn642.fusion.mixin;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.extensions.ResourceMetadataExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Created 09/10/2024 by SuperMartijn642
 */
@Mixin(targets = "net/minecraft/server/packs/resources/ResourceMetadata$Builder$1")
public class ResourceMetadata$BuilderMixin implements ResourceMetadataExtension {

    @Unique
    private boolean intervene = true;
    @Unique
    private ImmutableMap<MetadataSectionType<?>,Object> map;

    @Override
    public void disableFusionOverwrite(){
        this.intervene = false;
    }

    @ModifyVariable(
        method = "<init>",
        at = @At("TAIL"),
        ordinal = 0
    )
    private ImmutableMap<MetadataSectionType<?>,?> captureMap(ImmutableMap<MetadataSectionType<?>,Object> map){
        this.map = map;
        return map;
    }

    @Inject(
        method = "getSection",
        at = @At("HEAD"),
        cancellable = true
    )
    public void getSection(MetadataSectionType<?> serializer, CallbackInfoReturnable<Optional<?>> ci){
        // The entire sprite contents loading happens in SpriteResourceLoader, which is an interface
        // Forge's version of Mixin doesn't allow for mixins into static interface methods, so this is the best we can do

        // Make sure we always pass vanilla's frame size checks
        if(this.intervene && serializer == AnimationMetadataSection.TYPE && this.map.containsKey(FusionTextureMetadataSection.TYPE))
            ci.setReturnValue(Optional.empty());
    }
}
