package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.base.BaseBakedModel;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import com.supermartijn642.fusion.util.NotStupidItemOverrides;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public abstract class SimpleModelType<T> implements ModelType<T> {

    @Override
    public @Nullable IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, T data){
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
            // Get model properties for the quads
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
            List<BaseBakedModel.Quad>[] quads = new List[7];
            for(int i = 0; i < quads.length; i++)
                quads[i] = new ArrayList<>();
            ModelGeometry.QuadConsumer quadConsumer = (quad, cullDirection, geometryProperties) -> {
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                // Initialize the quad
                QuadProcessor<?> processor = null;
                if(sprite != null){
                    PropertyStore properties = FallbackPropertyStore.create(propertyStore, PropertyGetter.compose(geometryProperties, modelProperties));
                    processor = sprite.getTexture().initializeModelQuad(quad, sprite, properties);
                    SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(newSprite != null)
                        sprite = newSprite;
                }
                // Apply model properties
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                // Add the quad
                //noinspection unchecked
                quads[CullingHelper.cullIndex(cullDirection)].add(
                    new BaseBakedModel.Quad(
                        quad,
                        sprite,
                        (QuadProcessor<Object>)processor
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
            // Resolve ambient occlusion
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            if(ambientOcclusion == null)
                ambientOcclusion = true;
            // Resolve gui light
            BlockModel.GuiLight guiLight = modelStack.findGuiLightIncludingParents(context);
            if(guiLight == null)
                guiLight = BlockModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemCameraTransforms.TransformType,ItemTransformVec3f,ItemTransformVec3f> itemTransformResolver = (type, fallback) -> {
                ItemTransformVec3f transform = modelStack.findItemTransformIncludingParents(type, context);
                return transform == null ? fallback : transform;
            };
            ItemCameraTransforms itemTransforms = new ItemCameraTransforms(
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.NO_TRANSFORM),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.NO_TRANSFORM)
            );
            // Bake item overrides
            ItemOverrideList itemOverrides = new NotStupidItemOverrides(
                this.getItemOverrides(data),
                location -> {
                    UntypedModelInstance model = context.getModelOrMissing(location);
                    return model.bakeModel(context, ModelStack.empty().push(model, location));
                }
            );
            // Create the model
            return new BaseBakedModel(
                BaseBakedModel.Quads.create(quads),
                conditions,
                propertyStore,
                particleSprite,
                ambientOcclusion,
                guiLight,
                geometry.isGui3d(),
                itemTransforms,
                itemOverrides
            );
        }

        // Bake parent
        ResourceLocation parent = this.getParent(data);
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
    }

    @Nullable
    protected abstract ResourceLocation getParent(T data);

    protected void bakeGeometry(ModelBakingContext context, ModelStack modelStack, T data,
                                ModelTransform transform, ModelGeometry.MaterialKeyResolver materialResolver,
                                ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }
}
