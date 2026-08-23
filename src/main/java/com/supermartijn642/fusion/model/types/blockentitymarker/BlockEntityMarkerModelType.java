package com.supermartijn642.fusion.model.types.blockentitymarker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.client.resources.model.SpecialModels;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
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

    private static final ModelInstance<?> BLOCK_ENTITY_MARKER;

    static{
        BLOCK_ENTITY_MARKER = SpecialModels.BLOCK_ENTITY_MARKER instanceof BlockModel ?
            ModelInstance.of(DefaultModelTypes.CUBOID, (BlockModel)SpecialModels.BLOCK_ENTITY_MARKER) :
            ModelInstance.of(DefaultModelTypes.UNKNOWN, SpecialModels.BLOCK_ENTITY_MARKER);
    }

    @Override
    public Collection<ResourceLocation> getDependencies(Void data){
        return BLOCK_ENTITY_MARKER.getDependencies();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(Void data){
        return BLOCK_ENTITY_MARKER.getParents();
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(Void data){
        return BLOCK_ENTITY_MARKER.getAmbientOcclusion();
    }

    @Override
    public @Nullable BlockModel.GuiLight getGuiLight(Void data){
        return BLOCK_ENTITY_MARKER.getGuiLight();
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type, Void data){
        return BLOCK_ENTITY_MARKER.getItemTransform(type);
    }

    @Override
    public List<ItemOverride> getItemOverrides(Void data){
        return BLOCK_ENTITY_MARKER.getItemOverrides();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(Void data){
        return BLOCK_ENTITY_MARKER.getMaterials();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(Void data){
        return BLOCK_ENTITY_MARKER.getGeometry();
    }

    @Override
    public @Nullable Boolean getShade(Void data){
        return BLOCK_ENTITY_MARKER.getShade();
    }

    @Override
    public @Nullable Boolean getEmissive(Void data){
        return BLOCK_ENTITY_MARKER.getEmissive();
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, Void data){
        return BLOCK_ENTITY_MARKER.getProperty(property, context);
    }

    @Override
    public @Nullable BakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, Void data){
        // Item transforms
        ItemTransforms itemTransforms = SimpleModelType.resolveItemTransforms(context, modelStack);
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
