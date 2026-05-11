package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ModelBakingContextImpl implements ModelBakingContext {

    private final Consumer<String> warnings;
    private final ResourceLocation identifier;
    private final ModelTransform transform;
    private final BakedModel missingModel;
    private final Function<Material,TextureAtlasSprite> textureGetter;
    private final Map<ResourceLocation,ModelInstance<?>> dependencies;

    public ModelBakingContextImpl(Consumer<String> warnings, ResourceLocation identifier, ModelTransform transform, BakedModel missingModel, Function<Material,TextureAtlasSprite> textureGetter, Map<ResourceLocation,ModelInstance<?>> dependencies){
        this.warnings = warnings;
        this.identifier = identifier;
        this.transform = transform;
        this.missingModel = missingModel;
        this.textureGetter = textureGetter;
        this.dependencies = dependencies;
    }

    @Override
    public void pushWarning(String warning){
        this.warnings.accept(warning);
    }

    @Override
    public ResourceLocation getModelIdentifier(){
        return this.identifier;
    }

    @Override
    public ModelTransform getTransformation(){
        return this.transform;
    }

    @Override
    public TextureAtlasSprite getMaterial(ModelMaterial material){
        return this.textureGetter.apply(material.toMaterial());
    }

    @Override
    public @Nullable ModelInstance<?> getModel(ResourceLocation identifier){
        return this.dependencies.get(identifier);
    }

    @Override
    public BakedModel getMissingModel(){
        return this.missingModel;
    }
}
