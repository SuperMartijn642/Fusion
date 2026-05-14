package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.ModelInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Context for baking block state models.
 * <p>
 * Created 27/04/2023 by SuperMartijn642
 * @see com.supermartijn642.fusion.api.model.ModelType#bakeModel(ModelBakingContext, Object)
 */
@ApiStatus.NonExtendable
public interface ModelBakingContext {

    /**
     * Pushes a user-facing warning message that should be logged.
     */
    void pushWarning(String warning);

    /**
     * Gets the identifier of the top level model being baked.
     */
    ResourceLocation getModelIdentifier();

    /**
     * Gets the transformations that should be applied to the model.
     */
    ModelTransform getTransformation();

    /**
     * Gets the sprite for the given material.
     * If the material's sprite is not present, a warning will be logged and the missing sprite material is returned.
     * @param material the unresolved material
     */
    TextureAtlasSprite getMaterial(ModelMaterial material);

    /**
     * Gets the model corresponding to the given identifier.
     * @param identifier identifier for the model
     */
    @Nullable
    ModelInstance<?> getModel(ResourceLocation identifier);

    /**
     * Walks the model tree of the given model.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param modelInstance starting model, i.e. the model at the head of the tree
     * @param walker        consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    default <T> Optional<T> walkModelTree(UntypedModelInstance modelInstance, ModelWalker<T> walker){
        return ModelWalker.walkModelTree(this::getModel, modelInstance, walker);
    }

    /**
     * Walks the model tree of the given model.
     * The model tree is explored depth-first by recursively jumping to parent models.
     * @param model  identifier of the starting model, i.e. the model at the head of the tree
     * @param walker consumer for models in the tree
     * @return an optional value that was returned by given the walker
     */
    default <T> Optional<T> walkModelTree(ResourceLocation model, ModelWalker<T> walker){
        return ModelWalker.walkModelTree(this::getModel, model, walker);
    }
}
