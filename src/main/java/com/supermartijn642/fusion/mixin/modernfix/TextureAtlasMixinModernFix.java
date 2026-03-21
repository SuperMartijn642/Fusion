package com.supermartijn642.fusion.mixin.modernfix;

import com.supermartijn642.fusion.compat.modernfix.ModernFixTextureCreationHandler;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureAtlas.class, priority = 900)
public class TextureAtlasMixinModernFix {

    @Unique
    private List<TextureAtlasSprite.Info> fusionSpriteInfos = null;

    @Shadow
    private ResourceLocation getResourceLocation(ResourceLocation p_118325_){
        throw new AssertionError();
    }

    @ModifyVariable(
        method = "getBasicSpriteInfos(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At("HEAD"),
        ordinal = 0
    )
    private Set<ResourceLocation> interceptFusionTextures(Set<ResourceLocation> textures, ResourceManager resourceManager){
        this.fusionSpriteInfos = ModernFixTextureCreationHandler.onLoadTextures(resourceManager, textures);
        if(this.fusionSpriteInfos.isEmpty())
            return textures;
        textures = new HashSet<>(textures);
        for(TextureAtlasSprite.Info info : this.fusionSpriteInfos)
            textures.remove(info.name());
        return textures;
    }


    @Inject(
        method = "getBasicSpriteInfos(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Set;)Ljava/util/Collection;",
        at = @At("RETURN")
    )
    private void addFusionSprites(ResourceManager resourceManager, Set<ResourceLocation> textures, CallbackInfoReturnable<Collection<TextureAtlasSprite.Info>> ci){
        if(this.fusionSpriteInfos != null){
            ci.getReturnValue().addAll(this.fusionSpriteInfos);
            this.fusionSpriteInfos = null;
        }
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
