package com.supermartijn642.fusion.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.supermartijn642.core.generator.ResourceCache;
import com.supermartijn642.core.generator.ResourceGenerator;
import com.supermartijn642.core.generator.ResourceType;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.ModelInstance;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows generating model files for Fusion's model types.
 * Users must extend the class and overwrite {@link FusionModelProvider#generate()}.
 * Users may use {@link FusionModelProvider#addModel(ResourceLocation, ModelInstance)} to add models which should be generated.
 * <p>
 * Created 01/05/2023 by SuperMartijn642
 */
public abstract class FusionModelProvider extends ResourceGenerator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Map<ResourceLocation,ModelInstance<?>> models = new HashMap<>();

    /**
     * @param modid modid of the mod which creates the generator
     */
    public FusionModelProvider(String modid, ResourceCache cache){
        super(modid, cache);
    }

    @Override
    public void save(){
        for(Map.Entry<ResourceLocation,ModelInstance<?>> entry : this.models.entrySet()){
            ResourceLocation location = entry.getKey();
            ModelInstance<?> model = entry.getValue();
            this.cache.saveJsonResource(ResourceType.ASSET, FusionModelTypeRegistry.serializeModelData(model), location.getResourceDomain(), "models", location.getResourcePath());
        }
    }

    /**
     * Adds models which should be generated through {@link #addModel(ResourceLocation, ModelInstance)}.
     */
    public abstract void generate();

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
