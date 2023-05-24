package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ModelBakingContextImpl implements ModelBakingContext {

    private final ModelBakery modelBakery;
    private final Function<ResourceLocation,TextureAtlasSprite> spriteGetter;
    private final ISprite modelState;
    private final ResourceLocation modelIdentifier;

    public ModelBakingContextImpl(ModelBakery modelBakery, Function<ResourceLocation,TextureAtlasSprite> spriteGetter, ISprite modelState, ResourceLocation modelIdentifier){
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
        return this.spriteGetter.apply(identifier.getTexture());
    }

    @Override
    public TextureAtlasSprite getTexture(ResourceLocation atlas, ResourceLocation texture){
        return this.spriteGetter.apply(texture);
    }

    @Override
    public ISprite getTransformation(){
        return this.modelState;
    }

    @Override
    public ResourceLocation getModelIdentifier(){
        return this.modelIdentifier;
    }
}
