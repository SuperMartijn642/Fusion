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
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

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
    public Collection<Identifier> getDependencies(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<Identifier,UntypedModelInstance>> getParents(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.ambientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(T data){
        return data.guiLight();
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        ItemTransforms transforms = data.transforms();
        if(transforms == null)
            return null;
        ItemTransform transform = type.isModded() ?
            transforms.moddedTransforms().get(type) :
            transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return convertTextureSlots(data.textureSlots());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        UnbakedGeometry geometry = data.geometry();
        return geometry == null ? null : ModelGeometry.of(geometry);
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
    public BlockStateModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, T data){
        // Bake geometry
        UnbakedGeometry geometry = data.geometry();
        if(geometry != null){
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
                public TextureAtlasSprite get(Material material, ModelDebugName name){
                    return context.getMaterial(ModelMaterial.of(material));
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String reference, ModelDebugName name){
                    return materialResolver.get(reference);
                }
            };
            ModelBaker modelBaker = new ModelBaker() {
                @Override
                public ResolvedModel getModel(Identifier location){
                    return context.getModelBaker().getModel(location);
                }

                @Override
                public BlockModelPart missingBlockModelPart(){
                    return context.getMissingBlockStateModelPart();
                }

                @Override
                public SpriteGetter sprites(){
                    return spriteGetter;
                }

                @Override
                public PartCache parts(){
                    return context.getModelBaker().parts();
                }

                @Override
                public <X> X compute(SharedOperationKey<X> key){
                    return context.getModelBaker().compute(key);
                }
            };
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Bake the geometry
            QuadCollection quads = geometry.bake(
                textureSlots,
                modelBaker,
                transforms.toModelState(),
                context.getModelIdentifier()::toString
            );
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Find ambient occlusion property
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Create the model
            return new SingleVariant(new SimpleModelWrapper(
                quads,
                ambientOcclusion == null || ambientOcclusion,
                particleSprite
            ));
        }

        // Bake parent
        Identifier parent = data.parent();
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeBlockStateModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
    }

    @Override
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack, T data){
        // Bake geometry
        UnbakedGeometry geometry = data.geometry();
        if(geometry != null){
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
                public TextureAtlasSprite get(Material material, ModelDebugName name){
                    return context.getMaterial(ModelMaterial.of(material));
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String reference, ModelDebugName name){
                    return materialResolver.get(reference);
                }
            };
            ModelBaker modelBaker = new ModelBaker() {
                @Override
                public ResolvedModel getModel(Identifier location){
                    return context.getModelBaker().getModel(location);
                }

                @Override
                public BlockModelPart missingBlockModelPart(){
                    return context.getMissingBlockStateModelPart();
                }

                @Override
                public SpriteGetter sprites(){
                    return spriteGetter;
                }

                @Override
                public PartCache parts(){
                    return context.getModelBaker().parts();
                }

                @Override
                public <X> X compute(SharedOperationKey<X> key){
                    return context.getModelBaker().compute(key);
                }
            };
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            // Bake the geometry
            QuadCollection quads = geometry.bake(
                textureSlots,
                modelBaker,
                transforms.toModelState(),
                context.getModelIdentifier()::toString
            );
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Resolve gui light
            UnbakedModel.GuiLight guiLight = modelStack.findGuiLightIncludingParents(context);
            if(guiLight == null)
                guiLight = UnbakedModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) -> {
                ItemTransform transform = modelStack.findItemTransformIncludingParents(type, context);
                return transform == null ? fallback : transform;
            };
            ImmutableMap.Builder<ItemDisplayContext,ItemTransform> moddedTransforms = ImmutableMap.builder();
            for(ItemDisplayContext type : ItemDisplayContext.values()){
                if(type.isModded()){
                    ItemTransform transform = itemTransformResolver.apply(type, null);
                    if(transform != null)
                        moddedTransforms.put(type, transform);
                }
            }
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.ON_SHELF, ItemTransform.NO_TRANSFORM),
                moddedTransforms.build()
            );
            // Create the model
            return new BlockModelWrapper(
                context.getTintSources(),
                quads.getAll(),
                new ModelRenderProperties(
                    guiLight.lightLikeBlock(),
                    particleSprite,
                    itemTransforms
                ),
                BlockModelWrapper.detectRenderType(quads.getAll())
            );
        }

        // Bake parent
        Identifier parent = data.parent();
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeItemModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
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
