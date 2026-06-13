package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.TextureAtlasMixinHelper;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = AtlasTexture.class, priority = 900)
public class TextureAtlasMixin {

    @Unique
    private final ConcurrentLinkedQueue<Object> fusionDummyTextures = new ConcurrentLinkedQueue<>();

    @Inject(
        method = {
            "lambda$getBasicSpriteInfos$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V",
            "lambda$func_215256_a$2(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/resources/IResourceManager;Ljava/util/concurrent/ConcurrentLinkedQueue;)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;<init>(Ljava/lang/String;Ljava/io/InputStream;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILHARD,
        remap = false
    )
    private void handleFusionTextures(ResourceLocation identifier, IResourceManager resourceManager, ConcurrentLinkedQueue<TextureAtlasSprite> queue, CallbackInfo ci, ResourceLocation ignore, IResource resource){
        if(TextureCreationHandler.onLoadTexture(identifier, resource, queue))
            ci.cancel();
    }

    @Inject(
        method = "getLoadedSprites",
        at = @At("HEAD"),
        cancellable = true
    )
    private void clearOldState(CallbackInfoReturnable<?> ci){
        this.fusionDummyTextures.clear();
    }

    @Inject(
        method = "getLoadedSprites",
        at = @At("TAIL"),
        cancellable = true
    )
    private void initializeTextures(IResourceManager resourceManager, Stitcher stitcher, CallbackInfoReturnable<List<TextureAtlasSprite>> ci){
        //noinspection DataFlowIssue
        AtlasTexture textureAtlas = (AtlasTexture)(Object)this;
        ConcurrentLinkedQueue<TextureAtlasSprite> sprites = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Set<Object> dummyTextures = new LinkedHashSet<>(this.fusionDummyTextures);
        for(Object dummySprites : dummyTextures){
            TextureCreationHandler.Result<CompletableFuture<Void>> result = TextureAtlasMixinHelper.onLoadSprite(dummySprites, textureAtlas, stitcher.getWidth(), stitcher.getHeight(), stitcher.mipLevel, sprites);
            if(result != null){
                ci.cancel();
                if(result.value() != null)
                    tasks.add(result.value());
            }
        }
        this.fusionDummyTextures.clear();
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        ci.getReturnValue().addAll(sprites);
    }

    @Inject(
        method = "load(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ignoreDummyTextures(IResourceManager resourceManager, TextureAtlasSprite sprite, CallbackInfoReturnable<Boolean> ci){
        if(TextureAtlasMixinHelper.isDummySprite(sprite)){
            this.fusionDummyTextures.add(TextureAtlasMixinHelper.getDummyParent(sprite));
            ci.cancel();
        }
    }
}
