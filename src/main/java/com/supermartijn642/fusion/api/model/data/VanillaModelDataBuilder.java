package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.model.types.vanilla.VanillaModelDataBuilderImpl;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.resources.Identifier;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public interface VanillaModelDataBuilder<T extends VanillaModelDataBuilder<T,S>, S> {

    static VanillaModelDataBuilder<?,CuboidModel> builder(){
        return new VanillaModelDataBuilderImpl();
    }

    /**
     * Sets the parent model.
     */
    T parent(Identifier parent);

    /**
     * Puts the given reference under the given key. These keys may be used when on faces for elements of this model or its parent's.
     */
    T material(String key, String reference);

    /**
     * Puts the given texture under the given key. These keys may be used when on faces for elements of this model or its parent's.
     */
    T material(String key, Identifier texture, boolean forceTranslucent);

    /**
     * Puts the given texture under the given key. These keys may be used when on faces for elements of this model or its parent's.
     */
    default T material(String key, Identifier texture){
        return this.material(key, texture, false);
    }

    S build();
}
