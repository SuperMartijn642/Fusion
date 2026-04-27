package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.extensions.ResourceMetadataExtension;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 27/04/2026 by SuperMartijn642
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(ForgeHooksClient.class)
public class ForgeHooksClientMixin {

    @Inject(
        method = "loadSpriteContents",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void handleFusionTextures(ResourceLocation identifier, Resource resource, FrameSize frameSize, NativeImage image, ResourceMetadata resourceMetadata, CallbackInfoReturnable<SpriteContents> ci){
        // The entire sprite contents loading happens in SpriteResourceLoader, which is an interface
        // Forge's version of Mixin doesn't allow for mixins into static interface methods, so this is the best we can do

        if(resourceMetadata instanceof ResourceMetadataExtension)
            ((ResourceMetadataExtension)resourceMetadata).disableFusionOverwrite();
        AnimationMetadataSection animationMetadata = resourceMetadata.getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        TextureCreationHandler.Result<SpriteContents> result = TextureCreationHandler.onLoadTexture(identifier, image, animationMetadata, resourceMetadata);
        if(result != null)
            ci.setReturnValue(result.value());
    }
}
