package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.PropertyStore;
import net.minecraft.world.item.ItemStack;

/**
 * Processor for quads in an item model.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 * @see TextureType#initializeItemModelQuad(MutableQuad, SpriteInstance, Object, PropertyStore)
 */
public interface ItemQuadProcessor<S> {

    /**
     * Extracts the texture state from the level.
     * The state object should not keep references to objects that are not thread-safe like the level or block entities.
     * @param stack      item stack being rendered
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    S extractState(ItemStack stack, PropertyStore properties);

    /**
     * Creates a key representing the resulting processed quad for the given state. States that produce the same processed quad, should return the same key such that the quad can be cached based on the key.
     * If the produced quad is not deterministic or would not be practical to cache, one should return {@code null}.
     * @param state      state obtained from {@link #extractState(ItemStack, PropertyStore)}
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    Object createGeometryKey(S state, PropertyStore properties);

    /**
     * Emits quads based on the state.
     * @param quad       emitter for quads
     * @param sprite     sprite of the quad
     * @param state      state obtained from {@link #extractState(ItemStack, PropertyStore)}
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    void processQuad(EmittableQuad quad, SpriteInstance sprite, S state, PropertyStore properties);
}
