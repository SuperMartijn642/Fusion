package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteIdentifierImpl implements SpriteIdentifier {

    public static final SpriteIdentifier MISSING = SpriteIdentifier.of(TextureAtlases.getBlocks(), MissingTextureSprite.getLocation());

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

    @Override
    public final boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof SpriteIdentifierImpl)) return false;

        SpriteIdentifierImpl that = (SpriteIdentifierImpl)o;
        return this.atlas.equals(that.atlas) && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.atlas.hashCode();
        result = 31 * result + this.texture.hashCode();
        return result;
    }
}
