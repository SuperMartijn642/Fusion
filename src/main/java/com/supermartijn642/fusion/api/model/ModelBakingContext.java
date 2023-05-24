package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ModelBakingContext {

    /**
     * @return the model bakery
     */
    ModelBakery getModelBakery();

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
    ISprite getTransformation();

    /**
     * @return the identifier of the model.
     */
    ResourceLocation getModelIdentifier();
}
