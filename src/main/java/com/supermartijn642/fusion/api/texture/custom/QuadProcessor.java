package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.PropertyStore;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Processor for quads.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 * @see TextureType#initializeModelQuad(MutableQuad, SpriteInstance, Object, PropertyStore)
 */
public interface QuadProcessor<S> {

    /**
     * Extract the texture state to use when there is no additional block or item context.
     * @param randomSupplier supplier for a random source
     * @param properties     property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    S extractState(Supplier<Random> randomSupplier, PropertyStore properties);

    /**
     * Extracts the texture state from the level.
     * The state object should not keep references to objects that are not thread-safe like the level or block entities.
     * @param level          level that the model is in
     * @param pos            block position of the model
     * @param state          block state for the model, may be different from the state at the given position in the given level
     * @param randomSupplier supplier for a random source
     * @param properties     property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    S extractState(@Nullable ILightReader level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<Random> randomSupplier, PropertyStore properties);

    /**
     * Extracts the texture state from the level.
     * The state object should not keep references to objects that are not thread-safe like the level or block entities.
     * @param stack          item stack being rendered
     * @param randomSupplier supplier for a random source
     * @param properties     property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    S extractState(ItemStack stack, Supplier<Random> randomSupplier, PropertyStore properties);

    /**
     * Creates a key representing the resulting processed quad for the given state. States that produce the same processed quad, should return the same key such that the quad can be cached based on the key.
     * If the produced quad is not deterministic or would not be practical to cache, one should return {@code null}.
     * @param state      state obtained from {@link #extractState(ILightReader, BlockPos, BlockState, Supplier, PropertyStore)}
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    @Nullable
    Object createGeometryKey(S state, PropertyStore properties);

    /**
     * Emits quads based on the state.
     * @param quad       emitter for quads
     * @param sprite     sprite of the quad
     * @param state      state obtained from {@link #extractState(ILightReader, BlockPos, BlockState, Supplier, PropertyStore)}
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties are released after rendering.
     */
    void processQuad(EmittableQuad quad, SpriteInstance sprite, S state, PropertyStore properties);
}
