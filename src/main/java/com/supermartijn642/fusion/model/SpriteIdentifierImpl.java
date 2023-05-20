package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteIdentifierImpl implements SpriteIdentifier {

    private final ResourceLocation atlas, texture;
    private Material material;

    public SpriteIdentifierImpl(ResourceLocation atlas, ResourceLocation texture){
        this.atlas = atlas;
        this.texture = texture;
    }

    public SpriteIdentifierImpl(Material material){
        this(material.atlasLocation(), material.texture());
        this.material = material;
    }

    @Override
    public ResourceLocation getAtlas(){
        return this.atlas;
    }

    @Override
    public ResourceLocation getTexture(){
        return this.texture;
    }

    @Override
    public Material toMaterial(){
        return this.material == null ? (this.material = SpriteIdentifier.super.toMaterial()) : this.material;
    }
}
