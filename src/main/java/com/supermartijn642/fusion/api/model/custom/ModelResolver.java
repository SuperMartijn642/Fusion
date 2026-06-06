package com.supermartijn642.fusion.api.model.custom;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
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
     * @see ModelBakery#MISSING_MODEL_LOCATION
     */
    ResourceLocation MISSING_MODEL = ModelBakery.MISSING_MODEL_LOCATION;

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
