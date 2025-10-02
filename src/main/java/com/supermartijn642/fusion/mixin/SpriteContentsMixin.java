package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.TextureErrorException;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.ResourceMetadataExtension;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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

    @ModifyVariable(
        method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Object;<init>()V",
            shift = At.Shift.AFTER
        ),
        ordinal = 0
    )
    private FrameSize initMetadata(FrameSize originalSize, ResourceLocation identifier, FrameSize ignore, NativeImage image, Optional<AnimationMetadataSection> animationMetadata, List<MetadataSectionType.WithValue<?>> resourceMetadata){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = null;
        for(MetadataSectionType.WithValue<?> entry : resourceMetadata){
            if(entry.type() == FusionTextureMetadataSection.TYPE){
                //noinspection unchecked
                metadata = (Pair<TextureType<Object>,Object>)entry.value();
                break;
            }
        }
        if(metadata != null){
            this.fusionTextureMetadata = metadata;
            // Get the animation metadata
            if(resourceMetadata instanceof ResourceMetadataExtension)
                ((ResourceMetadataExtension)resourceMetadata).disableFusionOverwrite();
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier, animationMetadata.orElse(null)), metadata.right());
            }catch(TextureErrorException e){
                FusionClient.LOGGER.error("Error for texture '{}': {}", identifier, e.getMessage());
                // TODO
                // There is no way to make this content loading fail when the frame size is incorrect as Forge's Mixin version doesn't
                // allow for mixins into interfaces and the entire sprite contents loading happens in a static interface method
                // So just give up and see what happens
                return originalSize;
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");

            // Replace the current size
            return new FrameSize(newSize.left(), newSize.right());
        }
        return originalSize;
    }
}
