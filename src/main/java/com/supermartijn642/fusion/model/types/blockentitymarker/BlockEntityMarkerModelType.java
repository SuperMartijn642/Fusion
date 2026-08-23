package com.supermartijn642.fusion.model.types.blockentitymarker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import com.supermartijn642.fusion.util.NotStupidItemOverrides;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created 23/08/2026 by SuperMartijn642
 */
public class BlockEntityMarkerModelType implements ModelType<Void> {

    @Override
    public Collection<ResourceLocation> getDependencies(Void data){
        return DefaultModelTypes.CUBOID.getDependencies(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(Void data){
        return DefaultModelTypes.CUBOID.getParents(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(Void data){
        return DefaultModelTypes.CUBOID.getAmbientOcclusion(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable BlockModel.GuiLight getGuiLight(Void data){
        return DefaultModelTypes.CUBOID.getGuiLight(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, Void data){
        return DefaultModelTypes.CUBOID.getItemTransform(type, ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public List<ItemOverride> getItemOverrides(Void data){
        return DefaultModelTypes.CUBOID.getItemOverrides(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(Void data){
        return DefaultModelTypes.CUBOID.getMaterials(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable ModelGeometry getGeometry(Void data){
        return DefaultModelTypes.CUBOID.getGeometry(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable Boolean getShade(Void data){
        return DefaultModelTypes.CUBOID.getShade(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable Boolean getEmissive(Void data){
        return DefaultModelTypes.CUBOID.getEmissive(ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, Void data){
        return DefaultModelTypes.CUBOID.getProperty(property, context, ModelBakery.BLOCK_ENTITY_MARKER);
    }

    @Override
    public @Nullable IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, Void data){
        // Item transforms
        ItemCameraTransforms itemTransforms = SimpleModelType.resolveItemTransforms(context, modelStack);
        // Item overrides
        ItemOverrideList itemOverrides = new NotStupidItemOverrides(
            this.getItemOverrides(data),
            location -> {
                UntypedModelInstance model = context.getModelOrMissing(location);
                return model.bakeModel(context, ModelStack.empty().push(model, location));
            }
        );
        // Particle material
        ModelGeometry.MaterialKeyResolver materialResolver = ModelGeometry.MaterialKeyResolver.fromKeyLookup(
            key -> modelStack.findMaterialIncludingParents(key, context),
            context::getMaterial,
            k -> {},
            keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
        );
        TextureAtlasSprite particleSprite = materialResolver.get("particle");
        if(ModelMaterial.isMissingSprite(particleSprite))
            context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
        // Gui light
        BlockModel.GuiLight guiLight = modelStack.findGuiLightIncludingParents(context);
        if(guiLight == null)
            guiLight = BlockModel.GuiLight.SIDE;
        // Create model
        return new BuiltInModel(
            itemTransforms,
            itemOverrides,
            particleSprite,
            guiLight.lightLikeBlock()
        );
    }

    @Override
    public Void deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize block entity marker!");
    }

    @Override
    public JsonObject serialize(Void data){
        throw new UnsupportedOperationException("Cannot serialize block entity marker!");
    }
}
