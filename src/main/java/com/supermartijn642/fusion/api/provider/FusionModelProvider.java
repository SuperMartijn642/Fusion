package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.ModelInstance;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows generating model files for Fusion's model types.
 * Users must extend the class and overwrite {@link FusionModelProvider#generate()}.
 * Users may use {@link FusionModelProvider#addModel(ResourceLocation, ModelInstance)} to add models which should be generated.
 * <p>
 * Created 01/05/2023 by SuperMartijn642
 */
public abstract class FusionModelProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Map<ResourceLocation,ModelInstance<?>> models = new HashMap<>();
    private final String modName;
    private final DataGenerator generator;

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionModelProvider(String modid, DataGenerator generator){
        this.modName = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modid);
        this.generator = generator;
    }

    @Override
    public final void run(HashCache cache) throws IOException{
        this.generate();

        Path output = this.generator.getOutputFolder();
        for(Map.Entry<ResourceLocation,ModelInstance<?>> entry : this.models.entrySet()){
            ResourceLocation location = entry.getKey();
            ModelInstance<?> model = entry.getValue();
            String extension = location.getPath().lastIndexOf(".") > location.getPath().lastIndexOf("/") ? "" : ".json";
            Path path = Path.of("assets", location.getNamespace(), "models", location.getPath() + extension);
            DataProvider.save(GSON, cache, FusionModelTypeRegistry.serializeModelData(model), output.resolve(path));
        }
    }

    /**
     * Adds models which should be generated through {@link #addModel(ResourceLocation, ModelInstance)}.
     */
    protected abstract void generate();

    /**
     * Adds a model to be generated.
     * @param location location of the model
     * @param model    model instance to be serialized
     */
    public final void addModel(ResourceLocation location, ModelInstance<?> model){
        ModelInstance<?> previousValue = this.models.put(location, model);
        if(previousValue != null)
            throw new RuntimeException("Duplicate model for '" + location + "'!");
    }

    @Override
    public String getName(){
        return "Fusion Model Provider: " + this.modName;
    }
}
