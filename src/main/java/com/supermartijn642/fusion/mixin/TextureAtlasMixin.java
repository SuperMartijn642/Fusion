package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.DummyTextureSpriteContents;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.ITextureMapPopulator;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(value = TextureMap.class, priority = 900)
public class TextureAtlasMixin {

    @Shadow
    @Final
    private Map<String,TextureAtlasSprite> mapRegisteredSprites;
    @Shadow(remap = false)
    @Final
    private java.util.Set<ResourceLocation> loadedSprites;
    @Shadow
    @Final
    private Map<String,TextureAtlasSprite> mapUploadedSprites;
    @Shadow
    @Final
    private List<TextureAtlasSprite> listAnimatedSprites;
    @Shadow
    private int mipmapLevels;

    @Shadow
    private ResourceLocation getResourceLocation(TextureAtlasSprite sprite){
        throw new AssertionError();
    }

    @Unique
    private static final Executor EXECUTOR = new ForkJoinPool();
    @Unique
    private final Set<String> fusionCreatedSprites = new HashSet<>();

    @Inject(
        method = "loadSprites(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/ITextureMapPopulator;)V",
        at = @At("TAIL")
    )
    private void updateSprites(IResourceManager resourceManager, ITextureMapPopulator spritePopulator, CallbackInfo ci){
        //noinspection DataFlowIssue
        TextureMap textureMap = (TextureMap)(Object)this;
        // Since we replace texture atlas sprites, we need to call this again so references to the old sprites in ModelBakery get overwritten
        spritePopulator.registerSprites(textureMap);
    }

    @Inject(
        method = "loadTextureAtlas(Lnet/minecraft/client/resources/IResourceManager;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Set;clear()V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleFusionTextures(IResourceManager resourceManager, CallbackInfo ci, int maxTextureSize, Stitcher stitcher){
        FusionTextureMetadataSection.registerMetadata();

        // Process textures with Fusion metadata
        Queue<TextureAtlasSprite> newSprites = new ConcurrentLinkedQueue<>();
        Set<String> toRemove = Collections.synchronizedSet(new HashSet<>());
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for(TextureAtlasSprite sprite : this.mapRegisteredSprites.values()){
            tasks.add(CompletableFuture.runAsync(() -> {
                ResourceLocation identifier = new ResourceLocation(sprite.getIconName());
                ResourceLocation location = this.getResourceLocation(sprite);
                if(TextureCreationHandler.onLoadTexture(identifier, location, resourceManager, newSprites::add))
                    toRemove.add(sprite.getIconName());
            }, EXECUTOR));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        // Overwrite/add sprites
        for(TextureAtlasSprite newSprite : newSprites){
            toRemove.remove(newSprite.getIconName());
            this.mapRegisteredSprites.put(newSprite.getIconName(), newSprite);
            this.loadedSprites.add(new ResourceLocation(newSprite.getIconName()));
            stitcher.addSprite(newSprite);
        }
        // Remove left-over sprites that were not replaced
        for(String s : toRemove)
            this.mapRegisteredSprites.remove(s);

        // Since we are adding more sprites than the bar has allocated steps for, we need to update it
        ProgressManager.ProgressBar bar = null;
        Iterator<ProgressManager.ProgressBar> iterator = ProgressManager.barIterator();
        while(iterator.hasNext()) bar = iterator.next();
        if(bar != null && this.mapRegisteredSprites.size() > bar.getSteps()){
            try{
                Field steps = ProgressManager.ProgressBar.class.getDeclaredField("steps");
                steps.setAccessible(true);
                steps.set(bar, this.mapRegisteredSprites.size());
            }catch(NoSuchFieldException | IllegalAccessException e){
                throw new RuntimeException(e);
            }
        }
    }

    @Redirect(
        method = "finishLoading(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/Stitcher;getStichSlots()Ljava/util/List;"
        )
    )
    private List<TextureAtlasSprite> initializeTextures(Stitcher stitcher){
        // Collect all Fusion texture sprites
        List<TextureAtlasSprite> sprites = new ArrayList<>(stitcher.getStichSlots());
        Set<DummyTextureSpriteContents> dummySprites = new HashSet<>();
        for(int i = sprites.size() - 1; i >= 0; i--){
            TextureAtlasSprite sprite = sprites.get(i);
            if(sprite instanceof DummyTextureSpriteContents.Child){
                dummySprites.add(((DummyTextureSpriteContents.Child)sprite).parent().getTopTexture());
                sprites.remove(i);
            }
        }
        // Create the sprites
        Queue<TextureAtlasSprite> newSprites = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for(DummyTextureSpriteContents dummySprite : dummySprites) // This needs to run on main thread for OpenGL context, so no parallelization :(
            TextureCreationHandler.onLoadSprite(dummySprite, stitcher.getCurrentWidth(), stitcher.getCurrentHeight(), this.mipmapLevels, newSprites::add);
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        // Add the new sprites
        for(TextureAtlasSprite newSprite : newSprites){
            this.mapRegisteredSprites.put(newSprite.getIconName(), newSprite);
            this.mapUploadedSprites.put(newSprite.getIconName(), newSprite);
            if(newSprite.hasAnimationMetadata())
                this.listAnimatedSprites.add(newSprite);
            this.fusionCreatedSprites.add(newSprite.getIconName());
        }
        // Return the non-fusion sprites
        return sprites;
    }

    @Redirect(
        method = "finishLoading(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;values()Ljava/util/Collection;"
        ),
        remap = false
    )
    private Collection<TextureAtlasSprite> excludeFusionSpriteFromMissing(Map<String,TextureAtlasSprite> nonStitchedSprites){
        if(this.fusionCreatedSprites.isEmpty())
            return nonStitchedSprites.values();
        List<TextureAtlasSprite> sprites = new ArrayList<>(nonStitchedSprites.size());
        for(Map.Entry<String,TextureAtlasSprite> entry : nonStitchedSprites.entrySet()){
            if(!this.fusionCreatedSprites.contains(entry.getKey()))
                sprites.add(entry.getValue());
        }
        return sprites;
    }
}
