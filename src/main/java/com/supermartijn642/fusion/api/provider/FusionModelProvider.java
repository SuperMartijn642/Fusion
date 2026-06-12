package com.supermartijn642.fusion.api.provider;

import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Allows generating model files for Fusion's model types.
 * Users must extend the class and overwrite {@link FusionModelProvider#generate()}.
 * Users may use {@link FusionModelProvider#addModel(ResourceLocation, ModelInstance)} to add models which should be generated.
 * <p>
 * Created 01/05/2023 by SuperMartijn642
 */
public abstract class FusionModelProvider implements DataProvider {

    private final Map<ResourceLocation,ModelInstance<?>> models = new HashMap<>();
    private final String modName;
    private final PackOutput output;
    private final ExistingFileHelper existingFileHelper;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionModelProvider(String modid, PackOutput output, ExistingFileHelper existingFileHelper){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.output = output;
        this.existingFileHelper = existingFileHelper;
    }

    /**
     * @param modid modid of the mod which creates the generator
     * @deprecated Use {@link #FusionModelProvider(String, PackOutput, ExistingFileHelper)} which includes the existing file helper
     */
    @Deprecated
    public FusionModelProvider(String modid, PackOutput output){
        this(modid, output, null);
    }

    @Override
    public final CompletableFuture<?> run(CachedOutput cache){
        this.generate();

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        Path output = this.output.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModelInstance<?>> entry : this.models.entrySet()){
            ResourceLocation location = entry.getKey();
            ModelInstance<?> model = entry.getValue();
            String extension = location.getPath().lastIndexOf(".") > location.getPath().lastIndexOf("/") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "models", location.getPath() + extension);
            tasks.add(DataProvider.saveStable(cache, FusionModelTypeRegistry.serializeModelData(model), output.resolve(path)));
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    /**
     * Adds models which should be generated through {@link #addModel(ResourceLocation, ModelInstance)}.
     */
    protected abstract void generate();

    /**
     * Adds a model to be generated.
     * @param location  location of the model
     * @param modelType type of the model
     * @param data      model data to be serialized
     */
    public final <T> void addModel(ResourceLocation location, ModelType<T> modelType, T data){
        ModelInstance<?> previousValue = this.models.put(location, ModelInstance.of(modelType, data));
        if(previousValue != null)
            throw new RuntimeException("Duplicate model for '" + location + "'!");
    }

    /**
     * Adds a model to be generated.
     * @param location location of the model
     * @param model    model instance to be serialized
     */
    public final void addModel(ResourceLocation location, ModelInstance<?> model){
        ModelInstance<?> previousValue = this.models.put(location, model);
        if(previousValue != null)
            throw new RuntimeException("Duplicate model for '" + location + "'!");
        if(this.existingFileHelper != null)
            this.existingFileHelper.trackGenerated(location, PackType.CLIENT_RESOURCES, ".json", "models");
    }

    @Override
    public String getName(){
        return "Fusion Model Provider: " + this.modName;
    }
}
