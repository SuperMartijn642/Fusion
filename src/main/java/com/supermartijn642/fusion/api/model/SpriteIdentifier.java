package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.model.SpriteIdentifierImpl;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface SpriteIdentifier {

    static SpriteIdentifier of(ResourceLocation atlas, ResourceLocation texture){
        return new SpriteIdentifierImpl(atlas, texture);
    }

    static SpriteIdentifier of(Material material){
        return new SpriteIdentifierImpl(material);
    }

    /**
     * @return the identifier for the missing texture sprite in the block atlas
     */
    static SpriteIdentifier missing(){
        return of(TextureAtlases.getBlocks(), MissingTextureAtlasSprite.getLocation());
    }

    ResourceLocation getAtlas();

    ResourceLocation getTexture();

    default Material toMaterial(){
        return new Material(this.getAtlas(), this.getTexture());
    }
}
