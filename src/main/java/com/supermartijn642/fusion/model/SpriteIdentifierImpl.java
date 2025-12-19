package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class SpriteIdentifierImpl implements SpriteIdentifier {

    public static final SpriteIdentifier MISSING = of(TextureAtlases.getBlocks(), MissingTextureAtlasSprite.getLocation());

    public static SpriteIdentifier of(Identifier atlas, Identifier texture){
        return new SpriteIdentifierImpl(atlas, texture);
    }

    public static SpriteIdentifier of(Material material){
        return new SpriteIdentifierImpl(material);
    }

    private final Identifier atlas, texture;
    private Material material;

    private SpriteIdentifierImpl(Identifier atlas, Identifier texture){
        this.atlas = atlas;
        this.texture = texture;
    }

    private SpriteIdentifierImpl(Material material){
        this(material.atlasLocation(), material.texture());
        this.material = material;
    }

    @Override
    public Identifier getAtlas(){
        return this.atlas;
    }

    @Override
    public Identifier getTexture(){
        return this.texture;
    }

    @Override
    public Material toMaterial(){
        return this.material == null ? (this.material = SpriteIdentifier.super.toMaterial()) : this.material;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof SpriteIdentifierImpl that)) return false;

        return this.atlas.equals(that.atlas) && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.atlas.hashCode();
        result = 31 * result + this.texture.hashCode();
        return result;
    }
}
