package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
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
    private final Function<RenderMaterial,TextureAtlasSprite> textureGetter;
    private final Map<ResourceLocation,UntypedModelInstance> dependencies;
    private final ModelBakery modelBaker;
    private IBakedModel missingModel;

    public ModelBakingContextImpl(Consumer<String> warnings, ResourceLocation identifier, ModelTransform transform, Function<RenderMaterial,TextureAtlasSprite> textureGetter, Map<ResourceLocation,UntypedModelInstance> dependencies, ModelBakery modelBaker){
        this.warnings = warnings;
        this.identifier = identifier;
        this.transform = transform;
        this.textureGetter = textureGetter;
        this.dependencies = dependencies;
        this.modelBaker = modelBaker;
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
        return this.textureGetter.apply(material.toRenderMaterial());
    }

    @Override
    public @Nullable ModelInstance<?> getModel(ResourceLocation identifier){
        UntypedModelInstance model = this.dependencies.get(identifier);
        return model instanceof ModelInstance<?> ? (ModelInstance<?>)model : null;
    }

    @Override
    public IBakedModel getMissingBakedModel(){
        if(this.missingModel == null)
            this.missingModel = this.modelBaker.bake(ModelResolver.MISSING_MODEL, ModelTransform.identity().toModelTransform());
        return this.missingModel;
    }

    public ModelBakery getModelBakery(){
        return this.modelBaker;
    }
}
