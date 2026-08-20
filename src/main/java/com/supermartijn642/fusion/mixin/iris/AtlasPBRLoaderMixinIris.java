package com.supermartijn642.fusion.mixin.iris;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.integration.iris.IrisPBRTextureCreationHandler;
import net.irisshaders.iris.pbr.loader.AtlasPBRLoader;
import net.irisshaders.iris.pbr.texture.PBRAtlasTexture;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Created 18/08/2026 by SuperMartijn642
 */
@Mixin(AtlasPBRLoader.class)
public class AtlasPBRLoaderMixinIris {

    @Unique
    private final Set<TextureInstance<?>> processedTextures = new HashSet<>();

    private AtlasPBRLoaderMixinIris(){
    }

    @Inject(
        method = "load",
        at = {
            @At("HEAD"),
            @At("TAIL")
        },
        expect = 2
    )
    private void clearProcessedTextures(CallbackInfo ci){
        IrisPBRTextureCreationHandler.clear();
    }

    @WrapOperation(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/pbr/loader/AtlasPBRLoader;createPBRSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/client/renderer/texture/TextureAtlas;IIILnet/irisshaders/iris/pbr/texture/PBRType;)Lnet/irisshaders/iris/pbr/loader/AtlasPBRLoader$PBRTextureAtlasSprite;"
        ),
        require = 2
    )
    private AtlasPBRLoader.PBRTextureAtlasSprite loadFusionPBRTextures(AtlasPBRLoader loader, TextureAtlasSprite sprite, ResourceManager resourceManager, TextureAtlas atlas, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType, Operation<AtlasPBRLoader.PBRTextureAtlasSprite> operation,
                                                                       @Local(ordinal = 0) LocalRef<PBRAtlasTexture> normalAtlas, @Local(ordinal = 1) LocalRef<PBRAtlasTexture> specularAtlas){
        TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
        if(textureInstance == null)
            return operation.call(loader, sprite, resourceManager, atlas, atlasWidth, atlasHeight, mipLevel, pbrType);
        LocalRef<PBRAtlasTexture> pbrAtlas = pbrType == PBRType.NORMAL ? normalAtlas : specularAtlas;
        Supplier<PBRAtlasTexture> pbrAtlasSupplier = () -> {
            PBRAtlasTexture a = pbrAtlas.get();
            if(a == null)
                pbrAtlas.set(a = new PBRAtlasTexture(atlas, pbrType));
            return a;
        };
        IrisPBRTextureCreationHandler.createPBRTexture(textureInstance, resourceManager, atlasWidth, atlasHeight, mipLevel, pbrType, pbrAtlasSupplier);
        return null;
    }
}
