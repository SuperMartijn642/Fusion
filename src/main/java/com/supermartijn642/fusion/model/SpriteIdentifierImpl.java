package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteIdentifierImpl implements SpriteIdentifier {

    private final ResourceLocation atlas, texture;

    public SpriteIdentifierImpl(ResourceLocation atlas, ResourceLocation texture){
        this.atlas = atlas;
        this.texture = texture;
    }

    @Override
    public ResourceLocation getAtlas(){
        return this.atlas;
    }

    @Override
    public ResourceLocation getTexture(){
        return this.texture;
    }
}
