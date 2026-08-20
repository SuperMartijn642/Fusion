package com.supermartijn642.fusion.mixin.oculus;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.integration.iris.IrisPBRTextureCreationHandler;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.texture.pbr.PBRAtlasTexture;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.loader.AtlasPBRLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Created 18/08/2026 by SuperMartijn642
 */
@Mixin(AtlasPBRLoader.class)
public class AtlasPBRLoaderMixinOculus {

    private AtlasPBRLoaderMixinOculus(){
    }

    @Inject(
        method = "load",
        at = {
            @At("HEAD"),
            @At("TAIL")
        },
        expect = 2,
        remap = false
    )
    private void clearProcessedTextures(CallbackInfo ci){
        IrisPBRTextureCreationHandler.clear();
    }

    @ModifyVariable(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/mixin/texture/TextureAtlasAccessor;getTexturesByName()Ljava/util/Map;",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0,
        remap = false
    )
    private PBRAtlasTexture loadFusionNormalTextures(PBRAtlasTexture normalAtlas, TextureAtlas atlas, ResourceManager resourceManager, PBRTextureLoader.PBRTextureConsumer pbrAtlasConsumer){
        return this.loadFusionPBRTextures(normalAtlas, atlas, resourceManager, PBRType.NORMAL);
    }

    @ModifyVariable(
        method = "load",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/mixin/texture/TextureAtlasAccessor;getTexturesByName()Ljava/util/Map;",
            shift = At.Shift.BEFORE
        ),
        ordinal = 1,
        remap = false
    )
    private PBRAtlasTexture loadFusionSpecularTextures(PBRAtlasTexture specularAtlas, TextureAtlas atlas, ResourceManager resourceManager, PBRTextureLoader.PBRTextureConsumer pbrAtlasConsumer){
        return this.loadFusionPBRTextures(specularAtlas, atlas, resourceManager, PBRType.SPECULAR);
    }

    @Unique
    private PBRAtlasTexture loadFusionPBRTextures(PBRAtlasTexture pbrAtlas, TextureAtlas atlas, ResourceManager resourceManager, PBRType pbrType){
        TextureAtlasAccessor atlasAccessor = (TextureAtlasAccessor)atlas;
        int atlasWidth = atlasAccessor.callGetWidth();
        int atlasHeight = atlasAccessor.callGetHeight();
        int mipLevel = atlasAccessor.getMipLevel();
        AtomicBoolean requestedPbrAtlas = new AtomicBoolean(false);
        Supplier<PBRAtlasTexture> pbrAtlasSupplier = Suppliers.memoize(() -> {
            requestedPbrAtlas.set(true);
            return pbrAtlas == null ? new PBRAtlasTexture(atlas, pbrType) : pbrAtlas;
        });
        for(TextureAtlasSprite sprite : atlasAccessor.getTexturesByName().values()){
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance == null)
                continue;
            IrisPBRTextureCreationHandler.createPBRTexture(textureInstance, resourceManager, atlasWidth, atlasHeight, mipLevel, pbrType, pbrAtlasSupplier);
        }
        return requestedPbrAtlas.get() ? pbrAtlasSupplier.get() : pbrAtlas;
    }

    @Inject(
        method = "createPBRSprite",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void cancelFusionTexturePBRSpriteCreation(TextureAtlasSprite sprite, ResourceManager resourceManager, TextureAtlas atlas, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType, CallbackInfoReturnable<AtlasPBRLoader.PBRTextureAtlasSprite> ci){
        TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
        if(textureInstance != null)
            ci.setReturnValue(null);
    }
}
