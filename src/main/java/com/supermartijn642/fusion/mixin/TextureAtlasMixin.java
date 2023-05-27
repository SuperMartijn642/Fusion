package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureMap.class, priority = 900)
public class TextureAtlasMixin {

    @Unique
    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> fusionTextureMetadata = new HashMap<>();
    @Unique
    private final ThreadLocal<IResource> textureResource = new ThreadLocal<>();

    @Inject(
        method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void captureTextureResource(Stitcher stitcher, IResourceManager resourceManager, ResourceLocation location, TextureAtlasSprite textureatlassprite, net.minecraftforge.fml.common.ProgressManager.ProgressBar bar, int j, int k, CallbackInfoReturnable<Integer> ci, ResourceLocation resourceLocation, IResource resource){
        this.textureResource.set(resource);
    }

    @Redirect(
        method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;loadSprite(Lnet/minecraft/client/renderer/texture/PngSizeInfo;Z)V"
        )
    )
    private void gatherMetadata(TextureAtlasSprite sprite, PngSizeInfo pngSizeInfo, boolean hasAnimation) throws IOException{
        // Get the fusion metadata
        FusionMetadataSection.registerMetadata();
        FusionMetadataSection.Data data = this.textureResource.get().getMetadata(FusionMetadataSection.INSTANCE.getSectionName());
        Pair<TextureType<Object>,Object> metadata = data == null ? null : data.pair;
        if(metadata != null){
            ResourceLocation identifier = new ResourceLocation(sprite.getIconName());
            this.fusionTextureMetadata.put(identifier, metadata);
            // Adjust the frame size
            Pair<Integer,Integer> newSize;
            try{
                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(pngSizeInfo.pngWidth, sprite.hasAnimationMetadata() ? pngSizeInfo.pngWidth : pngSizeInfo.pngHeight, pngSizeInfo.pngWidth, pngSizeInfo.pngHeight, identifier), metadata.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
            }
            if(newSize == null)
                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
            // Replace the current size
            ((TextureAtlasSpriteExtension)sprite).setTextureSize(pngSizeInfo.pngWidth, pngSizeInfo.pngHeight);
            pngSizeInfo.pngWidth = newSize.left();
            pngSizeInfo.pngHeight = newSize.right();
        }

        sprite.loadSprite(pngSizeInfo, hasAnimation);
    }

    @ModifyVariable(
        method = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/Stitcher;addSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            shift = At.Shift.BEFORE
        ),
        ordinal = 0
    )
    private TextureAtlasSprite replaceSprite(TextureAtlasSprite texture){
        // Replace sprite
        ResourceLocation identifier = new ResourceLocation(texture.getIconName());
        Pair<TextureType<Object>,Object> textureData = this.fusionTextureMetadata.get(identifier);
        if(textureData != null){
            // Create the sprite
            TextureAtlasSprite newTexture;
            //noinspection DataFlowIssue
            try(SpriteCreationContextImpl context = new SpriteCreationContextImpl(texture, (TextureMap)(Object)this)){
                newTexture = textureData.left().createSprite(context, textureData.right());
            }catch(Exception e){
                throw new RuntimeException("Encountered an exception whilst initialising texture '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "'!", e);
            }
            if(newTexture == null)
                throw new RuntimeException("Received null texture from texture type '" + TextureTypeRegistryImpl.getIdentifier(textureData.left()) + "' for texture '" + identifier + "'!");
            ((TextureAtlasSpriteExtension)newTexture).setFusionTextureType(textureData.left());
            // Replace the current texture
            return newTexture;
        }
        return texture;
    }

    @Inject(
        method = "loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V",
        at = @At("RETURN")
    )
    private void clearTextureData(CallbackInfo ci){
        this.fusionTextureMetadata.clear();
    }
}
