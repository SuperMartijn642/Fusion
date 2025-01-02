package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Object;<init>()V",
            shift = At.Shift.AFTER
        )
    )
    private void initMetadata(ResourceLocation identifier, FrameSize frameSize, NativeImage image, ResourceMetadata resourceMetadata, CallbackInfo ci){
        // Get the fusion metadata
        resourceMetadata.getSection(FusionTextureMetadataSection.TYPE).ifPresent(metadata -> this.fusionTextureMetadata = metadata);
    }
}
