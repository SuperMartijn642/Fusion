package com.supermartijn642.fusion.mixin.vintagefix;

import com.google.common.collect.Lists;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import com.supermartijn642.fusion.texture.FusionMetadataSection;
import com.supermartijn642.fusion.texture.SpriteCreationContextImpl;
import com.supermartijn642.fusion.texture.SpritePreparationContextImpl;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.VintageFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created 19/08/2023 by SuperMartijn642
 */
@Mixin(TextureMap.class)
public class TextureAtlasMixinVintageFix {

    @Shadow
    @Final
    private Map<String,TextureAtlasSprite> mapRegisteredSprites;
    @Shadow
    @Final
    private java.util.Set<ResourceLocation> loadedSprites;
    @Shadow
    private int mipmapLevels;

    @Shadow
    private ResourceLocation getResourceLocation(TextureAtlasSprite sprite){
        throw new AssertionError();
    }

    @Inject(
        method = "loadTextureAtlas(Lnet/minecraft/client/resources/IResourceManager;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Set;clear()V",
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void loadFusionSprites(IResourceManager resourceManager, CallbackInfo ci, int j, Stitcher stitcher){
        FusionMetadataSection.registerMetadata();
        List<CompletableFuture<?>> tasks = Lists.newArrayList();
        for(Map.Entry<String,TextureAtlasSprite> entry : this.mapRegisteredSprites.entrySet()){
            tasks.add(CompletableFuture.runAsync(() -> {
                TextureAtlasSprite sprite = entry.getValue();
                // Load the texture resource
                ResourceLocation location = this.getResourceLocation(sprite);
                try(IResource resource = resourceManager.getResource(location)){
                    if(resource != null){
                        // Get the fusion metadata
                        FusionMetadataSection.Data data = resource.getMetadata(FusionMetadataSection.INSTANCE.getSectionName());
                        Pair<TextureType<Object>,Object> metadata = data == null ? null : data.pair;
                        if(metadata != null){
                            ResourceLocation identifier = new ResourceLocation(sprite.getIconName());
                            // Adjust the frame size
                            Pair<Integer,Integer> newSize;
                            try{
                                newSize = metadata.left().getFrameSize(new SpritePreparationContextImpl(sprite.width, sprite.hasAnimationMetadata() ? sprite.width : sprite.height, sprite.width, sprite.height, identifier), metadata.right());
                            }catch(Exception e){
                                throw new RuntimeException("Encountered an exception whilst getting frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!", e);
                            }
                            if(newSize == null)
                                throw new RuntimeException("Received null frame size from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
                            // Replace the current size
                            ((TextureAtlasSpriteExtension)sprite).setTextureSize(sprite.width, sprite.height);
                            sprite.width = newSize.left();
                            sprite.height = newSize.right();

                            // If the texture is not square, it will have errored and we need to load it again
                            if(sprite.framesTextureData.isEmpty()){
                                BufferedImage bufferedImage = TextureUtil.readBufferedImage(resource.getInputStream());
                                ((TextureAtlasSpriteExtension)sprite).setTextureSize(bufferedImage.getWidth(), bufferedImage.getHeight());
                                int[][] pixels = new int[this.mipmapLevels + 1][];
                                pixels[0] = new int[bufferedImage.getWidth() * bufferedImage.getHeight()];
                                bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), pixels[0], 0, bufferedImage.getWidth());
                                sprite.framesTextureData.add(pixels);
                            }

                            // Create the new sprite
                            TextureAtlasSprite newTexture;
                            try(SpriteCreationContextImpl context = new SpriteCreationContextImpl(sprite, (TextureMap)(Object)this)){
                                newTexture = metadata.left().createSprite(context, metadata.right());
                            }catch(Exception e){
                                throw new RuntimeException("Encountered an exception whilst initialising texture '" + identifier + "' for texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "'!", e);
                            }
                            if(newTexture == null)
                                throw new RuntimeException("Received null texture from texture type '" + TextureTypeRegistryImpl.getIdentifier(metadata.left()) + "' for texture '" + identifier + "'!");
                            ((TextureAtlasSpriteExtension)newTexture).setFusionTextureType(metadata.left());
                            // Add the sprite to be stitched
                            synchronized(stitcher){
                                stitcher.addSprite(newTexture);
                            }

                            // Mark the sprite as loaded
                            this.loadedSprites.add(new ResourceLocation(entry.getKey()));
                        }
                    }
                }catch(FileNotFoundException ignore){
                }catch(IOException e){throw new RuntimeException(e);}
            }, VintageFix.WORKER_POOL));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
    }
}
