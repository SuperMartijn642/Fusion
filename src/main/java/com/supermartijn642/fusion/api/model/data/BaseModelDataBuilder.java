package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.model.types.base.BaseModelDataBuilderImpl;
import net.minecraft.util.ResourceLocation;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public interface BaseModelDataBuilder<T extends BaseModelDataBuilder<T,S>, S> extends VanillaModelDataBuilder<T,S> {

    static BaseModelDataBuilder<?,BaseModelData> builder(){
        return new BaseModelDataBuilderImpl();
    }

    /**
     * Adds a parent model.
     */
    @Override
    T parent(ResourceLocation parent);

    /**
     * Adds the given parent models.
     */
    T parents(ResourceLocation... parents);
}
