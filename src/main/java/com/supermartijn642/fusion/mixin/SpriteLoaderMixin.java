package com.supermartijn642.fusion.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = SpriteLoader.class, priority = 900)
public class SpriteLoaderMixin {

    @Inject(
        method = "loadSprite",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/metadata/animation/AnimationMetadataSection;calculateFrameSize(II)Lnet/minecraft/client/resources/metadata/animation/FrameSize;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void handleFusionTextures(ResourceLocation identifier, Resource resource, CallbackInfoReturnable<SpriteContents> ci, AnimationMetadataSection animationMetadata, NativeImage image) throws IOException{
        TextureCreationHandler.Result<SpriteContents> result = TextureCreationHandler.onLoadTexture(identifier, image, animationMetadata, resource.metadata());
        if(result != null)
            ci.setReturnValue(result.value());
    }

    @ModifyVariable(
        method = "stitch",
        at = @At("HEAD"),
        ordinal = 0
    )
    private List<SpriteContents> flattenSprites(List<SpriteContents> sprites){
        return TextureCreationHandler.onStitchSprites(sprites);
    }

    @Inject(
        method = "getStitchedSprites",
        at = @At("RETURN")
    )
    private void initializeTextures(Stitcher<SpriteContents> stitcher, int atlasWidth, int atlasHeight, CallbackInfoReturnable<Map<ResourceLocation,TextureAtlasSprite>> ci){
        TextureCreationHandler.afterLoadSprites(ci.getReturnValue(), atlasWidth, atlasHeight, stitcher);
    }
}
