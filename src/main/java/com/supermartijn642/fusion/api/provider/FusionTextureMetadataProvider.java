package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.core.generator.ResourceType;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.util.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows generating texture metadata files for Fusion's texture types.
 * Users must extend the class and overwrite {@link FusionTextureMetadataProvider#generate()}.
 * Users may use {@link FusionTextureMetadataProvider#addTextureMetadata(ResourceLocation, TextureType, Object)} to add metadata which should be generated.
 * <p>
 * Created 02/05/2023 by SuperMartijn642
 */
public abstract class FusionTextureMetadataProvider extends ResourceGenerator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> metadata = new HashMap<>();

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionTextureMetadataProvider(String modid, ResourceCache cache){
        super(modid, cache);
    }

    @Override
    public void save(){
        for(Map.Entry<ResourceLocation,Pair<TextureType<Object>,Object>> entry : this.metadata.entrySet()){
            ResourceLocation location = entry.getKey();
            Pair<TextureType<Object>,Object> metadata = entry.getValue();
            String extension = location.getResourcePath().endsWith(".mcmeta") ? "" : location.getResourcePath().lastIndexOf('.') > location.getResourcePath().lastIndexOf('/') ? ".mcmeta" : ".png.mcmeta";
            JsonObject json = new JsonObject();
            json.add("fusion", FusionTextureTypeRegistry.serializeTextureData(metadata.left(), metadata.right()));
            this.cache.saveResource(ResourceType.ASSET, GSON.toJson(json).getBytes(StandardCharsets.UTF_8), location.getResourceDomain(), "textures", location.getResourcePath(), extension);
        }
    }

    /**
     * Adds texture metadata which should be generated through {@link #addTextureMetadata(ResourceLocation, TextureType, Object)}.
     */
    public abstract void generate();

    /**
     * Adds texture metadata to be generated.
     * @param location    location of the texture
     * @param textureType type of the texture
     * @param data        metadata to be serialized
     */
    public final <T> void addTextureMetadata(ResourceLocation location, TextureType<T> textureType, T data){
        //noinspection unchecked
        Pair<TextureType<Object>,Object> previousValue = this.metadata.put(location, Pair.of((TextureType<Object>)textureType, (Object)data));
        if(previousValue != null)
            throw new RuntimeException("Duplicate texture metadata for '" + location + "'!");
    }

    @Override
    public String getName(){
        return "Fusion Texture Metadata Provider: " + this.modName;
    }
}
