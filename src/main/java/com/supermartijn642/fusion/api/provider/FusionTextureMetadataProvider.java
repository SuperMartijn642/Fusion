package com.supermartijn642.fusion.api.provider;

import com.google.gson.JsonObject;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.TextureType;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Allows generating texture metadata files for Fusion's texture types.
 * Users must extend the class and overwrite {@link FusionTextureMetadataProvider#generate()}.
 * Users may use {@link FusionTextureMetadataProvider#addTextureMetadata(Identifier, TextureType, Object)} to add metadata which should be generated.
 * <p>
 * Created 02/05/2023 by SuperMartijn642
 */
public abstract class FusionTextureMetadataProvider implements DataProvider {

    private final Map<Identifier,RawTextureInstance<Object,?>> metadata = new HashMap<>();
    private final String modName;
    private final PackOutput output;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionTextureMetadataProvider(String modid, PackOutput output){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.output = output;
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache){
        this.generate();

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path output = this.output.getOutputFolder();
        for(Map.Entry<Identifier,RawTextureInstance<Object,?>> entry : this.metadata.entrySet()){
            Identifier location = entry.getKey();
            RawTextureInstance<Object,?> metadata = entry.getValue();
            String extension = location.getPath().endsWith(".mcmeta") ? "" : location.getPath().lastIndexOf('.') > location.getPath().lastIndexOf('/') ? ".mcmeta" : ".png.mcmeta";
            Path path = Path.of("assets", location.getNamespace(), "textures", location.getPath() + extension);
            JsonObject json = new JsonObject();
            json.add("fusion", FusionTextureTypeRegistry.serializeTextureData(metadata));
            tasks.add(DataProvider.saveStable(cache, json, output.resolve(path)));
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    /**
     * Adds texture metadata which should be generated through {@link #addTextureMetadata(Identifier, TextureType, Object)}.
     */
    protected abstract void generate();

    /**
     * Adds texture metadata to be generated.
     * @param location    location of the texture
     * @param textureType type of the texture
     * @param data        metadata to be serialized
     */
    public final <T> void addTextureMetadata(Identifier location, TextureType<T,?> textureType, T data){
        //noinspection unchecked
        RawTextureInstance<Object,?> previousValue = this.metadata.put(location, (RawTextureInstance<Object,?>)RawTextureInstance.of(textureType, data));
        if(previousValue != null)
            throw new RuntimeException("Duplicate texture metadata for '" + location + "'!");
    }

    /**
     * Adds texture metadata to be generated.
     * @param location location of the texture
     * @param texture  texture instance to be serialized
     */
    public final <T> void addTextureMetadata(Identifier location, RawTextureInstance<?,?> texture){
        //noinspection unchecked
        RawTextureInstance<Object,?> previousValue = this.metadata.put(location, (RawTextureInstance<Object,?>)texture);
        if(previousValue != null)
            throw new RuntimeException("Duplicate texture metadata for '" + location + "'!");
    }

    @Override
    public String getName(){
        return "Fusion Texture Metadata Provider: " + this.modName;
    }
}
