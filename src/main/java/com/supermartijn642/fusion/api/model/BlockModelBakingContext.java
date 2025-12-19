package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface BlockModelBakingContext {

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
    default TextureAtlasSprite getTexture(Identifier atlas, Identifier texture){
        return this.getTexture(SpriteIdentifier.of(atlas, texture));
    }

    /**
     * Gets the sprite for the given texture on the block atlas.
     * @param texture texture identifier
     */
    default TextureAtlasSprite getBlockTexture(Identifier texture){
        return this.getTexture(TextureAtlases.getBlocks(), texture);
    }

    /**
     * @return the transformations which should be applied to the model
     */
    ModelState getTransformation();

    /**
     * @return the identifier of the model.
     */
    Identifier getModelIdentifier();

    /**
     * Gets the model corresponding to the given identifier.
     * Only models which were returned from {@link ModelType#getModelDependencies(Object)} may be requested.
     * @param identifier identifier for the model
     */
    @Nullable
    ModelInstance<?> getModel(Identifier identifier);

    /**
     * Gets the resolved texture reference data for the model stack.
     * This is resolved by vanilla, so it may not be accurate for some models such as models with multiple parents.
     */
    Map<String,Material> getTopLevelTextureReferences();

    /**
     * Gets the resolved ambient occlusion for the model stack.
     */
    boolean getTopLevelAmbientOcclusion();

    /**
     * Gets the resolved gui lighting for the model stack.
     */
    boolean getTopLevelUseBlockLighting();

    /**
     * Gets the resolved item transforms for the model stack.
     */
    ItemTransforms getTopLevelItemTransforms();

    /**
     * Get the resolved geometry for the model stack.
     */
    UnbakedGeometry getTopLevelGeometry();
}
