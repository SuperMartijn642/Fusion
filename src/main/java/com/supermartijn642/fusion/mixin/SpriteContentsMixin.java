package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Created 12/09/2023 by SuperMartijn642
 */
@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsExtension {

    @Unique
    private Pair<TextureType<Object>,Object> fusionTextureMetadata;

    @Override
    public Pair<TextureType<Object>,Object> fusionTextureMetadata(){
        return this.fusionTextureMetadata;
    }

    @Override
    public void clearFusionTextureMetadata(){
        this.fusionTextureMetadata = null;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(
        method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;)V",
        at = @At("TAIL")
    )
    private void initMetadata(ResourceLocation identifier, FrameSize frameSize, NativeImage image, Optional<AnimationMetadataSection> animationMetadata, List<MetadataSectionType.WithValue<?>> resourceMetadata, CallbackInfo ci){
        // Get the fusion metadata
        for(MetadataSectionType.WithValue<?> entry : resourceMetadata){
            if(entry.type() == FusionTextureMetadataSection.TYPE){
                //noinspection unchecked
                this.fusionTextureMetadata = (Pair<TextureType<Object>,Object>)entry.value();
                break;
            }
        }
    }
}
