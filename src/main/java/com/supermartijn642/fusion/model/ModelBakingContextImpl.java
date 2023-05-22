package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ModelBakingContextImpl implements ModelBakingContext {

    private final ModelBakery modelBakery;
    private final Function<Material,TextureAtlasSprite> spriteGetter;
    private final IModelTransform modelState;
    private final ResourceLocation modelIdentifier;

    public ModelBakingContextImpl(ModelBakery modelBakery, Function<Material,TextureAtlasSprite> spriteGetter, IModelTransform modelState, ResourceLocation modelIdentifier){
        this.modelBakery = modelBakery;
        this.spriteGetter = spriteGetter;
        this.modelState = modelState;
        this.modelIdentifier = modelIdentifier;
    }


    @Override
    public ModelBakery getModelBakery(){
        return this.modelBakery;
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
    public IModelTransform getTransformation(){
        return this.modelState;
    }

    @Override
    public ResourceLocation getModelIdentifier(){
        return this.modelIdentifier;
    }
}
