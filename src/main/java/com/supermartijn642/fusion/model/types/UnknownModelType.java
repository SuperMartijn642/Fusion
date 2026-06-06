package com.supermartijn642.fusion.model.types;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.custom.geometry.ModelGeometryImpl;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

    public static final Map<Direction,List<BakedQuad>> EMPTY_CULLED_QUADS;

    static{
        ImmutableMap.Builder<Direction,List<BakedQuad>> builder = ImmutableMap.builder();
        for(Direction direction : Direction.values())
            builder.put(direction, List.of());
        EMPTY_CULLED_QUADS = builder.build();
    }

    public static Map<String,Either<String,ModelMaterial>> convertTextureSlots(TextureSlots.Data textureSlots){
        if(textureSlots.values().isEmpty())
            return Map.of();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builderWithExpectedSize(textureSlots.values().size());
        for(Map.Entry<String,TextureSlots.SlotContents> entry : textureSlots.values().entrySet()){
            if(entry.getValue() instanceof TextureSlots.Reference(String key))
                builder.put(entry.getKey(), Either.left(key));
            else if(entry.getValue() instanceof TextureSlots.Value(Material material))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(material)));
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(T data){
        return List.of();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(T data){
        return List.of();
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.getAmbientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(T data){
        return data.getGuiLight();
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        ItemTransforms transforms = data.getTransforms();
        if(transforms == null)
            return null;
        ItemTransform transform = transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return convertTextureSlots(data.getTextureSlots());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        return ModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, T data){
        // Resolve materials
        Set<String> missingKeys = new HashSet<>();
        ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
            key -> modelStack.findMaterialIncludingParents(key, context),
            context::getMaterial,
            missingKeys::add,
            keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
        );
        TextureSlots textureSlots = ModelGeometryImpl.createTextureSlots(materialResolver);
        // Create dummy model baker
        SpriteGetter spriteGetter = new SpriteGetter() {
            @Override
            public TextureAtlasSprite get(Material material){
                return context.getMaterial(ModelMaterial.of(material));
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String reference){
                return materialResolver.get(reference);
            }
        };
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public BakedModel bake(ResourceLocation location, ModelState modelState){
                return context.getModelBaker().bake(location, modelState);
            }

            @Override
            public SpriteGetter sprites(){
                return spriteGetter;
            }

            @Override
            public ModelDebugName rootName(){
                return null;
            }
        };
        // Compose transformations
        ModelTransform transforms = modelStack.composeTransforms();
        transforms = ModelTransform.compose(transforms, context.getTransformation());
        // Find ambient occlusion property
        Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
        // Resolve gui light
        UnbakedModel.GuiLight guiLight = modelStack.findGuiLightIncludingParents(context);
        if(guiLight == null)
            guiLight = UnbakedModel.GuiLight.SIDE;
        // Resolve item transforms
        BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) -> {
            ItemTransform transform = modelStack.findItemTransformIncludingParents(type, context);
            return transform == null ? fallback : transform;
        };
        ItemTransforms itemTransforms = new ItemTransforms(
            itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
            itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM)
        );
        // Bake the model
        BakedModel bakedModel = data.bake(
            textureSlots,
            modelBaker,
            transforms.toModelState(),
            ambientOcclusion == null || ambientOcclusion,
            guiLight.lightLikeBlock(),
            itemTransforms
        );
        if(!missingKeys.isEmpty())
            context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
        return bakedModel;
    }

    @Override
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack, T data){
        // Bake the block model
        BakedModel bakedModel = this.bakeBlockStateModel(context, modelStack, data);
        if(bakedModel == null)
            return null;

        // Create the item model
        return new BlockModelWrapper(
            bakedModel,
            context.getTintSources()
        );
    }

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }
}
