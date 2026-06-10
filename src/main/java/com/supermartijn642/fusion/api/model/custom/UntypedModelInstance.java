package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created 14/05/2026 by SuperMartijn642
 */
public interface UntypedModelInstance extends PropertyGetter {

    /**
     * Gets all the dependencies on other unbaked models.
     */
    Collection<Identifier> getDependencies();

    /**
     * Gets any parent models which the model may inherit properties from.
     */
    List<Either<Identifier,UntypedModelInstance>> getParents();

    /**
     * Gets whether the model should be rendered with ambient occlusion.
     */
    @Nullable
    Boolean getAmbientOcclusion();

    /**
     * Gets the lighting to use when the model is rendered in a gui.
     */
    @Nullable
    UnbakedModel.GuiLight getGuiLight();

    /**
     * Gets the transformations used to render the model as an item under the given context.
     */
    @Nullable
    ItemTransform getItemTransform(ItemDisplayContext type);

    /**
     * Gets the material references of the model.
     */
    Map<String,Either<String,ModelMaterial>> getMaterials();

    /**
     * Gets the material reference for the given key.
     */
    @Nullable
    default Either<String,ModelMaterial> getMaterial(String key){
        return this.getMaterials().get(key);
    }

    /**
     * Gets the geometry of the model.
     */
    @Nullable
    ModelGeometry getGeometry();

    /**
     * Gets whether the model should be shaded.
     */
    @Nullable
    Boolean getShade();

    /**
     * Gets whether the model is emissive.
     */
    @Nullable
    Boolean getEmissive();

    /**
     * Gets the transformations that should be applied to the model's geometry.
     */
    ModelTransform getTransform();

    /**
     * Gets the condition for this model.
     */
    @Nullable
    ModelPredicate getCondition();

    /**
     * Creates a block state model from the model data.
     */
    @Nullable
    BlockStateModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack);

    /**
     * Creates an item model from the model data.
     */
    @Nullable
    ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack);
}
