package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.*;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<CuboidModel> {

    @Override
    public BlockStateModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, CuboidModel data){
        // Bake geometry
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            PropertyGetter modelPropertyGetter = new PropertyGetter() {
                @Override
                public <X, C> Optional<X> getProperty(Property<X,C> property, C c){
                    return modelStack.findPropertyIncludingParents(property, c, context);
                }
            };
            // Initialize quads
            PropertyStore propertyStore = FallbackPropertyStore.create(modelPropertyGetter);
            quads = quads.mutateQuads((side, quad) -> {
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                // Initialize the quad
                if(sprite != null)
                    sprite.getTexture().initializeItemModelQuad(quad, sprite, propertyStore);
                // Apply properties
                if(ambientOcclusion != null)
                    quad.ambientOcclusion(ambientOcclusion);
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                return true;
            });
            // Resolve particle material
            ModelMaterial.Resolved particleMaterial = materialResolver.get("particle");
            if(particleMaterial.isMissing())
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Create the model
            return new SingleVariant(new SimpleModelWrapper(
                quads.toQuadCollection(),
                true, // Ambient occlusion is handled by quads themselves
                particleMaterial.toBakedMaterial()
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
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack, CuboidModel data){
        // Bake geometry
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            PropertyGetter modelPropertyGetter = new PropertyGetter() {
                @Override
                public <X, C> Optional<X> getProperty(Property<X,C> property, C c){
                    return modelStack.findPropertyIncludingParents(property, c, context);
                }
            };
            // Initialize quads
            PropertyStore propertyStore = FallbackPropertyStore.create(modelPropertyGetter);
            quads = quads.mutateQuads((side, quad) -> {
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                // Initialize the quad
                sprite.getTexture().initializeItemModelQuad(quad, sprite, propertyStore);
                if(ambientOcclusion != null)
                    quad.ambientOcclusion(ambientOcclusion);
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                return true;
            });
            // Resolve particle material
            ModelMaterial.Resolved particleMaterial = materialResolver.get("particle");
            if(particleMaterial.isMissing())
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
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.ON_SHELF, ItemTransform.NO_TRANSFORM)
            );
            // Create the model
            return new CuboidItemModelWrapper(
                context.getTintSources(),
                quads.toQuadCollection(),
                new ModelRenderProperties(
                    guiLight.lightLikeBlock(),
                    particleMaterial.toBakedMaterial(),
                    itemTransforms
                ),
                context.getTransformation().matrix()
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
    public CuboidModel deserialize(JsonObject json) throws JsonParseException{
        return CuboidModel.GSON.fromJson(json, CuboidModel.class);
    }

    @Override
    public JsonObject serialize(CuboidModel model){
        // Use base model type to serialize vanilla cuboid model
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder();
        // Copy properties
        builder.parent(model.parent())
            .guiLight(model.guiLight())
            .ambientOcclusion(model.ambientOcclusion())
            .itemTransforms(model.transforms());
        // Copy materials
        for(Map.Entry<String,TextureSlots.SlotContents> entry : model.textureSlots().values().entrySet()){
            String key = entry.getKey();
            switch(entry.getValue()){
                case TextureSlots.Reference reference -> builder.material(key, reference.target());
                case TextureSlots.Value value -> builder.material(key, ModelMaterial.of(value.material()));
            }
        }
        // Copy elements
        if(model.geometry() instanceof UnbakedCuboidGeometry geometry){
            for(CuboidModelElement element : geometry.elements())
                builder.elements(CuboidModelGeometry.Element.of(element));
        }
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
