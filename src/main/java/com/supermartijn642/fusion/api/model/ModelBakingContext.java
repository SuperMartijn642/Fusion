package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nullable;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ModelBakingContext {

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
    IModelState getTransformation();

    /**
     * @return the identifier of the model.
     */
    ResourceLocation getModelIdentifier();

    /**
     * Gets the model corresponding to the given identifier.
     * Only models which were returned from {@link ModelType#getModelDependencies(Object)} may be requested.
     * @param identifier identifier for the model
     */
    @Nullable
    ModelInstance<?> getModel(ResourceLocation identifier);
}
