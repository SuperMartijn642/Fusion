package com.supermartijn642.fusion.api.model;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;

import java.util.List;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface ItemModelBakingContext extends BlockModelBakingContext {

    /**
     * Gets the tint sources which were specified in the item info definition.
     */
    List<ItemTintSource> getTintSources();

    /**
     * Get the set of entity models.
     */
    EntityModelSet getEntityModels();
}
