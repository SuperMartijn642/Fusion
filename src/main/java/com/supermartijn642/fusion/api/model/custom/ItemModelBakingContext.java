package com.supermartijn642.fusion.api.model.custom;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Context for baking item models.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 * @see com.supermartijn642.fusion.api.model.ModelType#bakeItemModel(ItemModelBakingContext, Object)
 * @see BlockStateModelBakingContext
 */
@ApiStatus.NonExtendable
public interface ItemModelBakingContext extends BlockStateModelBakingContext {

    /**
     * Gets the tint sources that were specified in the item info definition.
     */
    List<ItemTintSource> getTintSources();

    /**
     * Gets the set of entity models.
     */
    EntityModelSet getEntityModels();
}
