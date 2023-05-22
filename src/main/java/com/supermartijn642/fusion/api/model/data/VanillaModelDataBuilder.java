package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.model.types.vanilla.VanillaModelDataBuilderImpl;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.util.ResourceLocation;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public interface VanillaModelDataBuilder<T extends VanillaModelDataBuilder<T,S>, S> {

    static VanillaModelDataBuilder<?,BlockModel> builder(){
        return new VanillaModelDataBuilderImpl();
    }

    /**
     * Sets the parent model.
     */
    T parent(ResourceLocation parent);

    /**
     * Puts the given reference under the given key. These keys may be used when on faces for elements of this model or its parent's.
     */
    T texture(String key, String reference);

    /**
     * Puts the given texture under the given key. These keys may be used when on faces for elements of this model or its parent's.
     */
    T texture(String key, ResourceLocation texture);

    S build();
}
