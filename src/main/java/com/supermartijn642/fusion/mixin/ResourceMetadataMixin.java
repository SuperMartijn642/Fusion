package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created 09/10/2024 by SuperMartijn642
 */
@Mixin(targets = "net/minecraft/server/packs/resources/ResourceMetadata$2")
public abstract class ResourceMetadataMixin implements ResourceMetadata {

    @Unique
    private boolean intervene;
    @Unique
    private boolean hasFusionMetadata;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(JsonObject json, CallbackInfo ci){
        if(json.has(FusionTextureMetadataSection.TYPE.name())){
            this.hasFusionMetadata = true;
            this.intervene = true;
        }
    }

    @Inject(
        method = "getSection",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSection(MetadataSectionType<?> serializer, CallbackInfoReturnable<Optional<?>> ci){
        // The entire sprite contents loading happens in SpriteResourceLoader, which is an interface
        // Forge's version of Mixin doesn't allow for mixins into static interface methods, so this is the best we can do

        // Make sure we always pass vanilla's frame size checks
        if(this.intervene && serializer == AnimationMetadataSection.TYPE)
            ci.setReturnValue(Optional.empty());
    }

    @Override
    public List<MetadataSectionType.WithValue<?>> getTypedSections(Collection<MetadataSectionType<?>> metadataSections){
        if(this.hasFusionMetadata && metadataSections.contains(FusionTextureMetadataSection.TYPE)){
            this.intervene = false;
            //noinspection DataFlowIssue
            List<MetadataSectionType.WithValue<?>> metadata = Streams.concat(
                metadataSections.stream(),
                Stream.of(AnimationMetadataSection.TYPE)
            ).map(this::getTypedSection).<MetadataSectionType.WithValue<?>>flatMap(Optional::stream).toList();
            this.intervene = true;
            return metadata;
        }
        return ResourceMetadata.super.getTypedSections(metadataSections);
    }
}
