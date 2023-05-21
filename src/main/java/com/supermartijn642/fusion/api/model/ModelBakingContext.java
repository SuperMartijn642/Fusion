package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ModelBakingContext {

    /**
     * @return the model baker
     */
    ModelBaker getModelBaker();

    /**
     * Gets the sprite for the given material.
     * @param identifier identifier for the sprite
     */
    TextureAtlasSprite getTexture(SpriteIdentifier identifier);

    /**
     * Gets the sprite for the given atlas and texture.
     * @param atlas   atlas which the texture is stitched to
     * @param texture texture identifier
     */
    default TextureAtlasSprite getTexture(ResourceLocation atlas, ResourceLocation texture){
        return this.getTexture(SpriteIdentifier.of(atlas, texture));
    }

    /**
     * Gets the sprite for the given texture on the block atlas.
     * @param texture texture identifier
     */
    default TextureAtlasSprite getBlockTexture(ResourceLocation texture){
        return this.getTexture(TextureAtlases.getBlocks(), texture);
    }

    /**
     * @return the transformations which should be applied to the model
     */
    ModelState getTransformation();

    /**
     * @return the identifier of the model.
     */
    ResourceLocation getModelIdentifier();
}
