package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Object;<init>()V",
            shift = At.Shift.AFTER
        ),
        ordinal = 0
    )
    private FrameSize initMetadata(FrameSize originalSize, ResourceLocation identifier, FrameSize ignore, NativeImage image, ResourceMetadata resourceMetadata){
        // Get the fusion metadata
        Pair<TextureType<Object>,Object> metadata = resourceMetadata.getSection(FusionTextureMetadataSection.INSTANCE).orElse(null);
        if(metadata != null){
            this.fusionTextureMetadata = metadata;
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(originalSize.width(), originalSize.height(), image.getWidth(), image.getHeight(), identifier), metadata.right());
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
