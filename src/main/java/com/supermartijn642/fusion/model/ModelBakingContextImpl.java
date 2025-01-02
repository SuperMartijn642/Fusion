package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;

import java.util.Map;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ModelBakingContextImpl implements ModelBakingContext {

    private final ModelBaker modelBaker;
    private final Function<Material,TextureAtlasSprite> spriteGetter;
    private final ModelState modelState;
    private final ResourceLocation modelIdentifier;
    private final Map<ResourceLocation,UnbakedModel> dependencies;
    private final Map<String,Material> topLevelTextureReferences;
    private final boolean topLevelAmbientOcclusion;
    private final boolean topLevelUseBlockLighting;
    private final ItemTransforms topLevelItemTransforms;
    private final ContextMap additionalProperties;

    public ModelBakingContextImpl(ModelBaker modelBaker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelIdentifier, Map<ResourceLocation,UnbakedModel> dependencies, Map<String,Material> topLevelTextureReferences, boolean topLevelAmbientOcclusion, boolean topLevelUseBlockLighting, ItemTransforms topLevelItemTransforms, ContextMap additionalProperties){
        this.modelBaker = modelBaker;
        this.spriteGetter = spriteGetter;
        this.modelState = modelState;
        this.modelIdentifier = modelIdentifier;
        this.dependencies = dependencies;
        this.topLevelTextureReferences = topLevelTextureReferences;
        this.topLevelAmbientOcclusion = topLevelAmbientOcclusion;
        this.topLevelUseBlockLighting = topLevelUseBlockLighting;
        this.topLevelItemTransforms = topLevelItemTransforms;
        this.additionalProperties = additionalProperties;
    }

    @Override
    public ModelBaker getModelBaker(){
        return this.modelBaker;
    }

    @Override
    public TextureAtlasSprite getTexture(SpriteIdentifier identifier){
        return this.spriteGetter.apply(identifier.toMaterial());
    }

    @Override
    public TextureAtlasSprite getTexture(ResourceLocation atlas, ResourceLocation texture){
        return this.spriteGetter.apply(new Material(atlas, texture));
    }

    @Override
    public ModelState getTransformation(){
        return this.modelState;
    }

    @Override
    public ResourceLocation getModelIdentifier(){
        return this.modelIdentifier;
    }

    @Override
    public ModelInstance<?> getModel(ResourceLocation identifier){
        if(!this.dependencies.containsKey(identifier))
            throw new IllegalStateException("Requesting model that was not given as a dependency!");
        return FusionBlockModel.getModelInstance(this.dependencies.get(identifier));
    }

    @Override
    public Map<String,Material> getTopLevelTextureReferences(){
        return this.topLevelTextureReferences;
    }

    @Override
    public boolean getTopLevelAmbientOcclusion(){
        return this.topLevelAmbientOcclusion;
    }

    @Override
    public boolean getTopLevelUseBlockLighting(){
        return this.topLevelUseBlockLighting;
    }

    @Override
    public ItemTransforms getTopLevelItemTransforms(){
        return this.topLevelItemTransforms;
    }

    @Override
    public ContextMap getNeoForgeAdditionalProperties(){
        return this.additionalProperties;
    }
}
