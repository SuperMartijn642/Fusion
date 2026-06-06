package com.supermartijn642.fusion.api.model.custom;

import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves models by identifier.
 * <p>
 * Created 06/06/2026 by SuperMartijn642
 */
@FunctionalInterface
public interface ModelResolver {

    /**
     * Location of the missing model.
     * @see ModelBakery#MODEL_MISSING
     */
    ResourceLocation MISSING_MODEL = ModelBakery.MODEL_MISSING;

    /**
     * Gets the model corresponding to the given identifier.
     * If the given identifier is {@link #MISSING_MODEL}, then the result must not be {@code null}.
     * @param identifier identifier for the model
     */
    @Nullable
    UntypedModelInstance getModel(ResourceLocation identifier);

    @ApiStatus.NonExtendable
    default UntypedModelInstance getModelOrMissing(ResourceLocation identifier){
        UntypedModelInstance model = this.getModel(identifier);
        if(model == null){
            model = this.getModel(MISSING_MODEL);
            if(model == null)
                throw new IllegalStateException("Could not get missing model!");
        }
        return model;
    }
}
