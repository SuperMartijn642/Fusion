package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.model.SpriteIdentifierImpl;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface SpriteIdentifier {

    static SpriteIdentifier of(ResourceLocation atlas, ResourceLocation texture){
        return SpriteIdentifierImpl.of(atlas, texture);
    }

    static SpriteIdentifier of(Material material){
        return SpriteIdentifierImpl.of(material);
    }

    /**
     * @return the identifier for the missing texture sprite in the block atlas
     */
    static SpriteIdentifier missing(){
        return SpriteIdentifierImpl.MISSING;
    }

    ResourceLocation getAtlas();

    ResourceLocation getTexture();

    default Material toMaterial(){
        return new Material(this.getAtlas(), this.getTexture());
    }
}
