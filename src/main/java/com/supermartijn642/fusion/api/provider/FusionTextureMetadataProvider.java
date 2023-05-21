package com.supermartijn642.fusion.api.provider;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows generating texture metadata files for Fusion's texture types.
 * Users must extend the class and overwrite {@link FusionTextureMetadataProvider#generate()}.
 * Users may use {@link FusionTextureMetadataProvider#addTextureMetadata(ResourceLocation, TextureType, Object)} to add metadata which should be generated.
 * <p>
 * Created 02/05/2023 by SuperMartijn642
 */
public abstract class FusionTextureMetadataProvider implements DataProvider {

    private final Map<ResourceLocation,Pair<TextureType<Object>,Object>> metadata = new HashMap<>();
    private final String modName;
    private final DataGenerator generator;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionTextureMetadataProvider(String modid, DataGenerator generator){
        this.modName = FabricLoader.getInstance().getModContainer(modid).map(ModContainer::getMetadata).map(ModMetadata::getName).orElse(modid);
        this.generator = generator;
    }

    @Override
    public final void run(CachedOutput cache) throws IOException{
        this.generate();

        Path output = this.generator.getOutputFolder();
        for(Map.Entry<ResourceLocation,Pair<TextureType<Object>,Object>> entry : this.metadata.entrySet()){
            ResourceLocation location = entry.getKey();
            Pair<TextureType<Object>,Object> metadata = entry.getValue();
            String extension = location.getPath().endsWith(".mcmeta") ? "" : location.getPath().lastIndexOf('.') > location.getPath().lastIndexOf('/') ? ".mcmeta" : ".png.mcmeta";
            Path path = Path.of("assets", location.getNamespace(), "textures", location.getPath() + extension);
            JsonObject json = new JsonObject();
            json.add("fusion", FusionTextureTypeRegistry.serializeTextureData(metadata.left(), metadata.right()));
            DataProvider.saveStable(cache, json, output.resolve(path));
        }
    }

    /**
     * Adds texture metadata which should be generated through {@link #addTextureMetadata(ResourceLocation, TextureType, Object)}.
     */
    protected abstract void generate();

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
