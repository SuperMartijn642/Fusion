package com.supermartijn642.fusion.compat.modernfix;

import com.google.common.collect.Lists;
import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created 03/04/2026 by SuperMartijn642
 */
public class ModernFixTextureCreationHandler {

    /**
     * Load any Fusion textures before ModernFix's mixin, then hide them from the set of texture locations.
     */
    public static List<TextureAtlasSprite.Info> onLoadTextures(ResourceManager resourceManager, Set<ResourceLocation> textures){
        List<CompletableFuture<?>> tasks = Lists.newArrayList();
        Queue<TextureAtlasSprite.Info> queue = new ConcurrentLinkedQueue<>();
        for(ResourceLocation texture : textures){
            tasks.add(CompletableFuture.runAsync(() -> {
                // Load the texture resource
                ResourceLocation location = new ResourceLocation(texture.getNamespace(), String.format(Locale.ROOT, "textures/%s%s", texture.getPath(), ".png"));
                if(!resourceManager.hasResource(location))
                    return;
                try(Resource resource = resourceManager.getResource(location)){
                    // Load texture regularly
                    TextureCreationHandler.onLoadTexture(texture, resource, queue);
                }catch(IOException ignore){
                    // If there's an error opening/reading the resource, let vanilla handle and complain about it
                }
            }, Util.backgroundExecutor()));
        }
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        return new ArrayList<>(queue);
    }
}
