package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType implements ModelType<ModelBlock> {

    public static final Map<EnumFacing,List<BakedQuad>> EMPTY_CULLED_QUADS;

    static{
        ImmutableMap.Builder<EnumFacing,List<BakedQuad>> builder = ImmutableMap.builder();
        for(EnumFacing direction : EnumFacing.values())
            builder.put(direction, Collections.emptyList());
        EMPTY_CULLED_QUADS = builder.build();
    }

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,String> materials){
        if(materials.isEmpty())
            return Collections.emptyMap();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builder();
        for(Map.Entry<String,String> entry : materials.entrySet()){
            if(IdentifierUtil.isValidIdentifier(entry.getValue()))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(new ResourceLocation(entry.getValue()))));
            else{
                String reference = entry.getValue();
                if(reference.startsWith("#"))
                    reference = reference.substring(1);
                builder.put(entry.getKey(), Either.left(reference));
            }
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(ModelBlock data){
        return data.ambientOcclusion ? null : false;
    }

    @Override
    public @Nullable Boolean getIsGui3d(ModelBlock data){
        return data.isGui3d();
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, ModelBlock data){
        ItemTransformVec3f transform = data.cameraTransforms.getTransform(type);
        return transform == ItemTransformVec3f.DEFAULT ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(ModelBlock data){
        return convertMaterials(data.textures);
    }

    @Override
    public ModelGeometry getGeometry(ModelBlock data){
        return data.elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(ModelBlock data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(ModelBlock data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, ModelBlock data){
        return Optional.empty();
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, ModelBlock data){
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
                    sprite.getTexture().initializeModelQuad(quad, sprite, propertyStore);
                // Apply properties
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                return true;
            });
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Resolve ambient occlusion
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            if(ambientOcclusion == null)
                ambientOcclusion = true;
            // Resolve gui3d
            Boolean isGui3d = modelStack.findIsGui3dIncludingParents(context);
            if(isGui3d == null)
                isGui3d = true;
            // Resolve item transforms
            BiFunction<ItemCameraTransforms.TransformType,ItemTransformVec3f,ItemTransformVec3f> itemTransformResolver = (type, fallback) -> {
                ItemTransformVec3f transform = modelStack.findItemTransformIncludingParents(type, context);
                return transform == null ? fallback : transform;
            };
            ItemCameraTransforms itemTransforms = new ItemCameraTransforms(
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.DEFAULT),
                itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.DEFAULT)
            );
            // Create the model
            return new SimpleBakedModel(
                quads.all().stream().map(QuadAccess::toBakedQuad).collect(Collectors.toList()),
                EMPTY_CULLED_QUADS,
                ambientOcclusion,
                isGui3d,
                particleSprite,
                itemTransforms,
                ItemOverrideList.NONE
            );
        }

        // Bake parent
        ResourceLocation parent = data.getParentLocation();
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
    }

    @Override
    public ModelBlock deserialize(JsonObject json) throws JsonParseException{
        return ModelBlock.SERIALIZER.fromJson(json, ModelBlock.class);
    }

    @Override
    public JsonObject serialize(ModelBlock model){
        // Use base model type to serialize vanilla cuboid model
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder();
        // Copy properties
        builder.parent(model.getParentLocation())
            .isGui3d(model.isGui3d())
            .ambientOcclusion(model.ambientOcclusion)
            .itemTransforms(model.cameraTransforms);
        // Copy materials
        for(Map.Entry<String,String> entry : model.textures.entrySet()){
            String value = entry.getValue();
            if(IdentifierUtil.isValidIdentifier(value))
                builder.material(entry.getKey(), new ResourceLocation(entry.getValue()));
            else{
                if(value.startsWith("#"))
                    value = value.substring(1);
                builder.material(entry.getKey(), value);
            }
        }
        // Copy elements
        for(BlockPart element : model.elements)
            builder.elements(CuboidModelGeometry.Element.of(element));
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
