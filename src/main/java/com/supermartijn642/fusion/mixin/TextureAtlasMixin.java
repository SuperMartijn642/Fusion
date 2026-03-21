package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = AtlasTexture.class, priority = 900)
public class TextureAtlasMixin {

    @Inject(
        method = "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;<init>(Ljava/lang/String;Ljava/io/InputStream;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleFusionTextures(ResourceLocation identifier, IResourceManager resourceManager, ConcurrentLinkedQueue<TextureAtlasSprite.Info> queue, CallbackInfo ci, ResourceLocation ignore, IResource resource){
        if(TextureCreationHandler.onLoadTexture(identifier, resource, queue))
            ci.cancel();
    }

    @Inject(
        method = "lambda$getLoadedSprites$4(ILjava/util/concurrent/ConcurrentLinkedQueue;Ljava/util/List;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Info;IIII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void initializeTextures(int mipmapLevels, ConcurrentLinkedQueue<TextureAtlasSprite> queue, List<CompletableFuture<?>> tasks, IResourceManager resourceManager, TextureAtlasSprite.Info spriteInfo, int atlasWidth, int atlasHeight, int spriteX, int spriteY, CallbackInfo ci){
        //noinspection DataFlowIssue
        AtlasTexture textureAtlas = (AtlasTexture)(Object)this;
        TextureCreationHandler.Result<CompletableFuture<Void>> result = TextureCreationHandler.onLoadSprite(spriteInfo, spriteX, spriteY, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, queue);
        if(result != null){
            ci.cancel();
            if(result.value() != null)
                tasks.add(result.value());
        }
    }
}
