package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureAtlas.class, priority = 900)
public class TextureAtlasMixin {

    @Inject(
        method = "lambda$getBasicSpriteInfos$2(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Queue;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/PngInfo;<init>(Ljava/lang/String;Ljava/io/InputStream;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleFusionTextures(ResourceLocation identifier, ResourceManager resourceManager, Queue<TextureAtlasSprite.Info> queue, CallbackInfo ci, ResourceLocation ignore, Resource resource){
        if(TextureCreationHandler.onLoadTexture(identifier, resource, queue))
            ci.cancel();
    }

    @Inject(
        method = "lambda$getLoadedSprites$4(ILjava/util/Queue;Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Info;IIII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void initializeTextures(int mipmapLevels, Queue<TextureAtlasSprite> queue, List<CompletableFuture<?>> tasks, ResourceManager resourceManager, TextureAtlasSprite.Info spriteInfo, int atlasWidth, int atlasHeight, int spriteX, int spriteY, CallbackInfo ci){
        //noinspection DataFlowIssue
        TextureAtlas textureAtlas = (TextureAtlas)(Object)this;
        TextureCreationHandler.Result<CompletableFuture<Void>> result = TextureCreationHandler.onLoadSprite(spriteInfo, spriteX, spriteY, textureAtlas, atlasWidth, atlasHeight, mipmapLevels, queue);
        if(result != null){
            ci.cancel();
            if(result.value() != null)
                tasks.add(result.value());
        }
    }
}
