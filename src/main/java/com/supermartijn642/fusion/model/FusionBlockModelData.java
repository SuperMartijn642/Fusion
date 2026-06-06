package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModelData extends BlockModel {

    public static final ThreadLocal<ResourceLocation> CURRENT_MODEL = new ThreadLocal<>();

    @Nullable
    public static FusionBlockModelData get(UnbakedModel model){
        return model instanceof FusionBlockModelData ? (FusionBlockModelData)model : null;
    }

    private final ResourceLocation identifier;
    private final UntypedModelInstance model;

    private FusionBlockModelData(ResourceLocation identifier, UntypedModelInstance model){
        super(null, List.of(), TextureSlots.Data.EMPTY, null, null, null);
        this.identifier = identifier;
        this.model = model;
    }

    public FusionBlockModelData(ModelInstance<?> model){
        super(null, List.of(), TextureSlots.Data.EMPTY, null, null, null);
        ResourceLocation name = CURRENT_MODEL.get();
        this.identifier = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
    }

    @Override
    public void resolveDependencies(Resolver resolver){
        // Dependencies
        this.model.getDependencies().forEach(resolver::resolve);

        // Fill vanilla block model properties
        List<com.supermartijn642.fusion.api.util.Either<ResourceLocation,UntypedModelInstance>> parents = this.model.getParents();
        this.parentLocation = parents.isEmpty() ? null : parents.getFirst().leftOrNull();
        if(!parents.isEmpty()){
            Either<ResourceLocation,UntypedModelInstance> parent = parents.getFirst();
            if(parent.isLeft()){
                this.parent = resolver.resolve(parent.left());
                this.parentLocation = parent.left();
            }else{
                FusionBlockModelData wrapper = new FusionBlockModelData(this.identifier.withSuffix("_parent"), parent.right());
                wrapper.resolveDependencies(resolver);
                this.parent = wrapper;
            }
        }
        this.elements = FusionBlockModelData.getElements(this.model);
        this.guiLight = this.model.getGuiLight();
        this.hasAmbientOcclusion = this.model.getAmbientOcclusion();
        this.transforms = FusionBlockModelData.getItemTransforms(this.model);
        this.textureSlots = FusionBlockModelData.getTextureSlots(this.model);
    }

    public BakedModel bakeBlockModel(ModelBaker modelBakery, ModelState modelState){
        return bakeBlockModel(this.model, this.identifier, modelBakery, modelState);
    }

    public static BakedModel bakeBlockModel(UntypedModelInstance modelInstance, ResourceLocation identifier, ModelBaker modelBakery, ModelState modelState){
        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        BlockStateModelBakingContext context = new BlockStateModelBakingContextImpl(
            warnings::add,
            identifier,
            ModelTransform.of(modelState),
            modelBakery
        );
        // Let the custom model handle the actual baking
        BakedModel bakedModel;
        try{
            bakedModel = modelInstance.bakeBlockStateModel(context, ModelStack.empty().push(modelInstance, identifier));
        }catch(Exception e){
            if(modelInstance instanceof ModelInstance<?>)
                throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)modelInstance).getModelType()) + "' for  '" + identifier + "'!", e);
            else
                throw new RuntimeException("Encountered an exception while baking untyped block model for '" + identifier + "'!", e);
        }
        if(bakedModel == null)
            bakedModel = context.getMissingBakedModel();
        // Log warnings
        if(!warnings.isEmpty())
            LoggingHelper.logUserWarnings(warnings, "Warnings for block model '{}':", identifier);
        return bakedModel;
    }

    public ItemModel bakeItemModel(List<ItemTintSource> tintSources, ModelBaker modelBakery, EntityModelSet entityModelSet){
        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        ItemModelBakingContext context = new ItemModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.identity(),
            modelBakery,
            tintSources,
            entityModelSet
        );
        // Let the custom model handle the actual baking
        ItemModel model;
        try{
            model = this.model.bakeItemModel(context, ModelStack.empty().push(this.model, this.identifier));
        }catch(Exception e){
            if(this.model instanceof ModelInstance<?>)
                throw new RuntimeException("Encountered an exception while baking item model of type '" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)this.model).getModelType()) + "' for  '" + this.identifier + "'!", e);
            else
                throw new RuntimeException("Encountered an exception while baking untyped item model for '" + this.identifier + "'!", e);
        }
        if(model == null)
            model = context.getMissingItemModel();
        // Log warnings
        if(!warnings.isEmpty())
            LoggingHelper.logUserWarnings(warnings, "Warnings for item model '{}':", this.identifier);

        return model;
    }

    @Override
    public BakedModel bake(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState, boolean ambientOcclusion, boolean isGui3d, ItemTransforms itemTransforms){
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder()
            .ambientOcclusion(ambientOcclusion)
            .itemTransforms(itemTransforms);
        textureSlots.resolvedValues.forEach((key, material) -> builder.material(key, material.texture()));
        return bakeBlockModel(
            new ModelInstanceImpl<>(DefaultModelTypes.BASE, builder.build()) {
                @Override
                public List<Either<ResourceLocation,UntypedModelInstance>> getParents(){
                    return List.of(Either.right(FusionBlockModelData.this.model));
                }
            },
            this.identifier,
            modelBaker,
            modelState
        );
    }

    private static List<BlockElement> getElements(UntypedModelInstance model){
        ModelGeometry geometry = model.getGeometry();
        if(!(geometry instanceof CuboidModelGeometry))
            return List.of();
        return ((CuboidModelGeometry)geometry).elements().stream().map(element -> {
            Map<Direction,BlockElementFace> faces = new EnumMap<>(Direction.class);
            for(Direction side : Direction.values()){
                CuboidModelGeometry.Face face = element.face(side);
                if(face == null)
                    continue;
                String material = face.material();
                if(!material.isEmpty() && material.charAt(0) == '#')
                    material = material.substring(1);
                faces.put(side, new BlockElementFace(
                    face.cullDirection(),
                    face.tintIndex() == null ? -1 : face.tintIndex(),
                    material,
                    new BlockFaceUV(
                        face.uv() == null ? null : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                        face.rotation() == null ? 0 : face.rotation().angle()
                    ),
                    new ExtraFaceData(
                        face.getProperty(DefaultModelProperties.NEO_GEOMETRY_COLOR).orElse(-1),
                        face.getProperty(DefaultModelProperties.NEO_GEOMETRY_BLOCK_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.NEO_GEOMETRY_SKY_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.NEO_GEOMETRY_AMBIENT_OCCLUSION).orElse(true)
                    ),
                    new MutableObject<>()
                ));
            }
            return new BlockElement(
                new Vector3f(element.from()), new Vector3f(element.to()),
                faces,
                element.rotation(),
                element.shade() == null || element.shade(),
                element.lightEmission() == null ? 0 : element.lightEmission(),
                new ExtraFaceData(
                    element.getProperty(DefaultModelProperties.NEO_GEOMETRY_COLOR).orElse(-1),
                    element.getProperty(DefaultModelProperties.NEO_GEOMETRY_BLOCK_LIGHT).orElse(0),
                    element.getProperty(DefaultModelProperties.NEO_GEOMETRY_SKY_LIGHT).orElse(0),
                    element.getProperty(DefaultModelProperties.NEO_GEOMETRY_AMBIENT_OCCLUSION).orElse(true)
                )
            );
        }).toList();
    }

    public static TextureSlots.Data getTextureSlots(UntypedModelInstance model){
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        model.getMaterials().forEach((key, value) -> {
            if(value.isLeft())
                textures.addReference(key, value.left());
            else
                textures.addTexture(key, value.right().toMaterial());
        });
        return textures.build();
    }

    private static ItemTransforms getItemTransforms(UntypedModelInstance model){
        ImmutableMap.Builder<ItemDisplayContext,ItemTransform> transformsBuilder = ImmutableMap.builder();
        for(ItemDisplayContext type : ItemDisplayContext.values()){
            ItemTransform transform = model.getItemTransform(type);
            if(transform != null)
                transformsBuilder.put(type, transform);
        }
        ImmutableMap<ItemDisplayContext,ItemTransform> transforms = transformsBuilder.build();
        return new ItemTransforms(
            transforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
            transforms
        );
    }

    public static UntypedModelInstance getModelInstance(UnbakedModel model){
        if(model instanceof FusionBlockModelData)
            return ((FusionBlockModelData)model).model;
        if(model instanceof BlockModel){
            ModelInstance<?> modelInstance = ((BlockModelExtension)model).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.CUBOID, (BlockModel)model);
                ((BlockModelExtension)model).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        if(model.getClass().equals(ItemModelGenerator.class))
            return ModelInstance.of(DefaultModelTypes.ITEM_MODEL_GENERATOR, (ItemModelGenerator)model);
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
