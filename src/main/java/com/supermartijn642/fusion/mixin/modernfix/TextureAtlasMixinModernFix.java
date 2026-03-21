package com.supermartijn642.fusion.mixin.modernfix;

import com.supermartijn642.fusion.compat.modernfix.ModernFixTextureCreationHandler;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = AtlasTexture.class, priority = 900)
public class TextureAtlasMixinModernFix {

    @Unique
    private List<TextureAtlasSprite.Info> fusionSpriteInfos = null;

    @Shadow
    private ResourceLocation getResourceLocation(ResourceLocation location){
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "getBasicSpriteInfos(Lnet/minecraft/resources/IResourceManager;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At("HEAD"),
        ordinal = 0
    )
    private Set<ResourceLocation> interceptFusionTextures(Set<ResourceLocation> textures, IResourceManager resourceManager){
        this.fusionSpriteInfos = ModernFixTextureCreationHandler.onLoadTextures(resourceManager, textures);
        if(this.fusionSpriteInfos.isEmpty())
            return textures;
        textures = new HashSet<>(textures);
        for(TextureAtlasSprite.Info info : this.fusionSpriteInfos)
            textures.remove(info.name());
        return textures;
    }


    @Inject(
        method = "getBasicSpriteInfos(Lnet/minecraft/resources/IResourceManager;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At("RETURN")
    )
    private void gatherMetadata(IResourceManager resourceManager, Set<ResourceLocation> sprites, CallbackInfoReturnable<Collection<TextureAtlasSprite.Info>> ci){
        if(this.fusionSpriteInfos != null){
            ci.getReturnValue().addAll(this.fusionSpriteInfos);
            this.fusionSpriteInfos = null;
        }
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
