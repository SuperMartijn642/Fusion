package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.base.BaseBlockStateModel;
import com.supermartijn642.fusion.model.types.base.BaseItemModel;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public abstract class SimpleModelType<T> implements ModelType<T> {

    @Override
    public @Nullable BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, T data){
        // Bake geometry
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Create shared property store
            PropertyStore propertyStore = PropertyStore.create();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialKeyResolver materialResolver = ModelGeometry.MaterialKeyResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Combine conditions
            ModelPredicate conditions = modelStack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // NeoForge render type
            RenderTypeGroup neoRenderType = modelStack.findPropertyIncludingParents(DefaultModelProperties.NEO_MODEL_RENDER_TYPE, context).orElse(RenderTypeGroup.EMPTY);
            // Get model properties for the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            PropertyGetter modelProperties = new PropertyGetter() {
                @Override
                public <X, C> Optional<X> getProperty(Property<X,C> property, C c){
                    return modelStack.findPropertyIncludingParents(property, c, context);
                }
            };
            // Create quad consumer
            //noinspection unchecked
            List<BaseBlockStateModel.Quad>[] quads = new List[7];
            for(int i = 0; i < quads.length; i++)
                quads[i] = new ArrayList<>();
            ModelGeometry.QuadConsumer quadConsumer = (quad, cullDirection, geometryProperties) -> {
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                // NeoForge render type
                if(!neoRenderType.isEmpty())
                    quad.renderTypes(neoRenderType.block(), neoRenderType.entity());
                // Initialize the quad
                BlockStateQuadProcessor<?> processor = null;
                if(sprite != null){
                    PropertyStore properties = FallbackPropertyStore.create(propertyStore, PropertyGetter.compose(geometryProperties, modelProperties));
                    processor = sprite.getTexture().initializeBlockStateModelQuad(quad, sprite, properties);
                    SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(newSprite != null)
                        sprite = newSprite;
                }
                // Apply model properties
                if(ambientOcclusion != null)
                    quad.ambientOcclusion(ambientOcclusion);
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                // Add the quad
                //noinspection unchecked
                quads[CullingHelper.cullIndex(cullDirection)].add(
                    new BaseBlockStateModel.Quad(
                        quad,
                        sprite,
                        (BlockStateQuadProcessor<Object>)processor
                    )
                );
            };
            // Bake the geometry
            this.bakeGeometry(context, modelStack, data, transforms, materialResolver, quadConsumer);
            geometry.bake(quadConsumer, transforms, materialResolver);
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
                moddedTransforms.build()
            );// Create the model
            return new BaseBlockStateModel(
                new BaseBlockStateModel.Quads(quads),
                conditions,
                propertyStore,
                guiLight,
                particleSprite,
                itemTransforms
            );
        }

        // Bake parent
        ResourceLocation parent = this.getParent(data);
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
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Create shared property store
            PropertyStore propertyStore = PropertyStore.create();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialKeyResolver materialResolver = ModelGeometry.MaterialKeyResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Combine conditions
            ModelPredicate conditions = modelStack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // NeoForge render type
            RenderTypeGroup neoRenderType = modelStack.findPropertyIncludingParents(DefaultModelProperties.NEO_MODEL_RENDER_TYPE, context).orElse(RenderTypeGroup.EMPTY);
            // Get model properties for the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            PropertyGetter modelProperties = new PropertyGetter() {
                @Override
                public <X, C> Optional<X> getProperty(Property<X,C> property, C c){
                    return modelStack.findPropertyIncludingParents(property, c, context);
                }
            };
            // Create quad consumer
            List<BaseItemModel.Quad> quads = new ArrayList<>();
            ModelGeometry.QuadConsumer quadConsumer = (quad, cullDirection, geometryProperties) -> {
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                // NeoForge render type
                if(!neoRenderType.isEmpty())
                    quad.renderTypes(neoRenderType.block(), neoRenderType.entity());
                // Initialize the quad
                ItemQuadProcessor<?> processor = null;
                if(sprite != null){
                    PropertyStore properties = FallbackPropertyStore.create(propertyStore, PropertyGetter.compose(geometryProperties, modelProperties));
                    processor = sprite.getTexture().initializeItemModelQuad(quad, sprite, properties);
                    SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(newSprite != null)
                        sprite = newSprite;
                }
                // Apply model properties
                if(ambientOcclusion != null)
                    quad.ambientOcclusion(ambientOcclusion);
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                // Add the quad
                //noinspection unchecked
                quads.add(new BaseItemModel.Quad(
                    quad,
                    sprite,
                    (ItemQuadProcessor<Object>)processor
                ));
            };
            // Bake the geometry
            this.bakeGeometry(context, modelStack, data, transforms, materialResolver, quadConsumer);
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
                moddedTransforms.build()
            );
            // Create the model
            return new BaseItemModel(
                quads,
                conditions,
                propertyStore,
                guiLight,
                particleSprite,
                itemTransforms,
                context.getTintSources()
            );
        }

        // Bake parent
        ResourceLocation parent = this.getParent(data);
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeItemModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
    }

    @Nullable
    protected abstract ResourceLocation getParent(T data);

    protected void bakeGeometry(BlockStateModelBakingContext context, ModelStack modelStack, T data,
                                ModelTransform transform, ModelGeometry.MaterialKeyResolver materialResolver,
                                ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }
}
