package com.supermartijn642.fusion.api.model;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
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
     * Bakes the given material.
     * @param material material to bake
     */
    Material.Baked bakeMaterial(Material material);

    Material missingMaterial();

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

    ContextMap getNeoForgeAdditionalProperties();
}
