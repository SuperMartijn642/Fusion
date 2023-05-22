package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.model.SpriteIdentifierImpl;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface SpriteIdentifier {

    static SpriteIdentifier of(ResourceLocation atlas, ResourceLocation texture){
        return new SpriteIdentifierImpl(atlas, texture);
    }

    static SpriteIdentifier of(RenderMaterial material){
        return new SpriteIdentifierImpl(material);
    }

    /**
     * @return the identifier for the missing texture sprite in the block atlas
     */
    static SpriteIdentifier missing(){
        return of(TextureAtlases.getBlocks(), MissingTextureSprite.getLocation());
    }

    ResourceLocation getAtlas();

    ResourceLocation getTexture();

    default RenderMaterial toMaterial(){
        return new RenderMaterial(this.getAtlas(), this.getTexture());
    }
}
