package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.ForgeFaceData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModelData extends BlockModel {

    public static final ThreadLocal<ResourceLocation> CURRENT_MODEL = new ThreadLocal<>();
    public static Pair<UnbakedModel,BakedModel> missingModel;

    @Nullable
    public static FusionBlockModelData get(UnbakedModel model){
        return model instanceof FusionBlockModelData ? (FusionBlockModelData)model : null;
    }

    private final ResourceLocation identifier;
    private final ModelInstance<?> model;
    private List<UnbakedModel> parents = List.of();
    private Map<ResourceLocation,ModelInstance<?>> dependencies;

    private FusionBlockModelData(ResourceLocation identifier, ModelInstance<?> model){
        super(null, List.of(), Map.of(), true, null, ItemTransforms.NO_TRANSFORMS, List.of());
        this.identifier = identifier;
        this.model = model;
    }

    public FusionBlockModelData(ModelInstance<?> model){
        super(null, List.of(), Map.of(), true, null, ItemTransforms.NO_TRANSFORMS, List.of());
        ResourceLocation name = CURRENT_MODEL.get();
        this.identifier = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
    }

    @Override
    public void resolveParents(Function<ResourceLocation,UnbakedModel> resolver){
        // Dependencies
        Collection<ResourceLocation> dependencies = this.model.getDependencies();
        this.dependencies = new HashMap<>(dependencies.size());
        this.dependencies.put(this.identifier, this.model);
        // Track resolved models
        AtomicReference<Function<ResourceLocation,UnbakedModel>> trackingResolverHolder = new AtomicReference<>();
        Function<ResourceLocation,UnbakedModel> trackingResolver = new Function<>() {
            final Map<ResourceLocation,UnbakedModel> resolved = new HashMap<>();

            @Override
            public UnbakedModel apply(ResourceLocation identifier){
                UnbakedModel model = this.resolved.get(identifier);
                if(model == null){
                    model = resolver.apply(identifier);
                    model.resolveParents(trackingResolverHolder.get());
                    FusionBlockModelData.this.dependencies.put(identifier, getModelInstance(model));
                }
                return model;
            }
        };
        trackingResolverHolder.set(trackingResolver);
        collectDependencies(this.model, trackingResolver);

        // Parents
        List<com.supermartijn642.fusion.api.util.Either<ResourceLocation,ModelInstance<?>>> parents = this.model.getParents();
        this.parents = new ArrayList<>(parents.size());
        for(com.supermartijn642.fusion.api.util.Either<ResourceLocation,ModelInstance<?>> parent : parents){
            this.parents.add(
                parent.flatMap(
                    trackingResolver,
                    m -> {
                        UnbakedModel wrapper = new FusionBlockModelData(IdentifierUtil.withSuffix(this.identifier, "_parent"), m);
                        wrapper.resolveParents(trackingResolver);
                        return wrapper;
                    }
                )
            );
        }
        this.parents = List.copyOf(this.parents);

        // Finalize dependencies map
        this.dependencies = Map.copyOf(this.dependencies);

        // Fill vanilla block model properties
        this.parentLocation = parents.isEmpty() ? null : parents.get(0).leftOrNull();
        if(!this.parents.isEmpty() && this.parents.get(0) instanceof BlockModel)
            this.parent = (BlockModel)this.parents.get(0);
        this.elements = FusionBlockModelData.getElements(this.model);
        this.guiLight = this.model.getGuiLight();
        this.hasAmbientOcclusion = this.model.getAmbientOcclusion() == null || this.model.getAmbientOcclusion();
        this.transforms = FusionBlockModelData.getItemTransforms(this.model);
        this.textureMap = FusionBlockModelData.getMaterials(this.model);
    }

    private static void collectDependencies(ModelInstance<?> model, Function<ResourceLocation,UnbakedModel> resolver){
        for(ResourceLocation dependency : model.getDependencies()){
            ModelInstance<?> dependencyModel = FusionBlockModelData.getModelInstance(resolver.apply(dependency));
            collectDependencies(dependencyModel, resolver);
        }
    }

    @Override
    public BakedModel bake(ModelBaker modelBaker, Function<Material,TextureAtlasSprite> textureGetter, ModelState modelState, ResourceLocation topModelIdentifier){
        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        ModelBakingContext context = new ModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.of(modelState),
            missingModel.right(),
            textureGetter,
            this.dependencies,
            modelBaker
        );
        // Let the custom model handle the actual baking
        BakedModel bakedModel;
        try{
            bakedModel = this.model.bakeModel(context);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.identifier + "'!", e);
        }
        // Log warnings
        if(!warnings.isEmpty())
            LoggingHelper.logUserWarnings(warnings, "Warnings for block model '{}':", this.identifier);
        return bakedModel;
    }

    @Contract("_,_,_,_,!null -> !null")
    private static <T> T getFromModelTree(UnbakedModel model,
                                          Function<FusionBlockModelData,@Nullable T> fusionModelGetter,
                                          Function<ModelInstance<?>,@Nullable T> unknownModelGetter,
                                          BiFunction<T,T,T> merger,
                                          T defaultValue){
        T value = model instanceof FusionBlockModelData ?
            fusionModelGetter.apply((FusionBlockModelData)model) :
            unknownModelGetter.apply(FusionBlockModelData.getModelInstance(model));
        if(value != null)
            return value;
        // Check parents
        if(!(model instanceof FusionBlockModelData))
            return model instanceof BlockModel ?
                getFromModelTree(((BlockModel)model).parent, fusionModelGetter, unknownModelGetter, merger, defaultValue)
                : defaultValue;
        T mergedValue = null;
        for(UnbakedModel parent : ((FusionBlockModelData)model).parents){
            value = getFromModelTree(parent, fusionModelGetter, unknownModelGetter, merger, null);
            if(value != null){
                if(merger == null){
                    mergedValue = value;
                    break;
                }else
                    mergedValue = mergedValue == null ?
                        value :
                        merger.apply(mergedValue, value);
            }
        }
        return mergedValue == null ? defaultValue : mergedValue;
    }

    private static <T> T getFromModelTree(UnbakedModel model,
                                          Function<FusionBlockModelData,@Nullable T> fusionModelGetter,
                                          Function<ModelInstance<?>,@Nullable T> unknownModelGetter,
                                          T defaultValue){
        return getFromModelTree(model, fusionModelGetter, unknownModelGetter, null, defaultValue);
    }

    @Override
    public List<BlockElement> getElements(){
        return getFromModelTree(
            this,
            m -> m.elements.isEmpty() ? null : m.elements,
            FusionBlockModelData::getElements,
            (a, b) -> Stream.concat(a.stream(), b.stream()).toList(),
            List.of()
        );
    }

    @Override
    public boolean hasAmbientOcclusion(){
        return getFromModelTree(
            this,
            m -> m.model.getAmbientOcclusion() ,
            ModelInstance::getAmbientOcclusion,
            true
        );
    }

    @Override
    public GuiLight getGuiLight(){
        return getFromModelTree(
            this,
            m -> m.guiLight,
            ModelInstance::getGuiLight,
            GuiLight.SIDE
        );
    }

    @Override
    public ItemTransforms getTransforms(){
        ImmutableMap.Builder<ItemTransforms.TransformType,ItemTransform> builder = ImmutableMap.builder();
        for(ItemTransforms.TransformType type : ItemTransforms.TransformType.values()){
            if(type.isModded()){
                ItemTransform transform = this.getTransform(type);
                if(transform != ItemTransform.NO_TRANSFORM)
                    builder.put(type, transform);
            }
        }
        return new ItemTransforms(
            this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND),
            this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND),
            this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND),
            this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND),
            this.getTransform(ItemTransforms.TransformType.HEAD),
            this.getTransform(ItemTransforms.TransformType.GUI),
            this.getTransform(ItemTransforms.TransformType.GROUND),
            this.getTransform(ItemTransforms.TransformType.FIXED),
            builder.build()
        );
    }

    @Override
    public ItemTransform getTransform(ItemTransforms.TransformType type){
        return getFromModelTree(
            this,
            m -> m.transforms.hasTransform(type) ? m.transforms.getTransform(type) : null,
            m -> m.getItemTransform(type),
            ItemTransform.NO_TRANSFORM
        );
    }

    @Override
    public com.mojang.datafixers.util.Either<Material,String> findTextureEntry(String key){
        com.mojang.datafixers.util.Either<Material,String> material = getFromModelTree(
            this,
            m -> m.textureMap.get(key),
            m -> {
                com.supermartijn642.fusion.api.util.Either<String,ModelMaterial> entry = m.getMaterial(key);
                return entry == null ? null : entry.flatMap(
                    com.mojang.datafixers.util.Either::right,
                    mm -> com.mojang.datafixers.util.Either.left(mm.toMaterial())
                );
            },
            null
        );
        if(material == null)
            return com.mojang.datafixers.util.Either.left(ModelMaterial.missingBlockAtlas().toMaterial());
        return material;
    }

    private static List<BlockElement> getElements(ModelInstance<?> model){
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
                    new ForgeFaceData(
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_COLOR).orElse(-1),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_BLOCK_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_SKY_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_AMBIENT_OCCLUSION).orElse(true)
                    )
                ));
            }
            return new BlockElement(
                new Vector3f(element.from()), new Vector3f(element.to()),
                faces,
                element.rotation(),
                element.shade() == null || element.shade()
            );
        }).toList();
    }

    private static Map<String,Either<Material,String>> getMaterials(ModelInstance<?> model){
        Map<String,Either<Material,String>> materials = new HashMap<>();
        model.getMaterials().forEach((key, value) -> {
            if(value.isLeft())
                materials.put(key, Either.right(value.left()));
            else
                materials.put(key, Either.left(value.right().toMaterial()));
        });
        return Map.copyOf(materials);
    }

    private static ItemTransforms getItemTransforms(ModelInstance<?> model){
        ImmutableMap.Builder<ItemTransforms.TransformType,ItemTransform> transformsBuilder = ImmutableMap.builder();
        for(ItemTransforms.TransformType type : ItemTransforms.TransformType.values()){
            ItemTransform transform = model.getItemTransform(type);
            if(transform != null)
                transformsBuilder.put(type, transform);
        }
        ImmutableMap<ItemTransforms.TransformType,ItemTransform> transforms = transformsBuilder.build();
        return new ItemTransforms(
            transforms.getOrDefault(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.HEAD, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.GUI, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.GROUND, ItemTransform.NO_TRANSFORM),
            transforms.getOrDefault(ItemTransforms.TransformType.FIXED, ItemTransform.NO_TRANSFORM),
            transforms
        );
    }

    public static ModelInstance<?> getModelInstance(UnbakedModel model){
        if(model instanceof FusionBlockModelData)
            return ((FusionBlockModelData)model).model;
        if(model == ModelBakery.GENERATION_MARKER)
            return ModelInstance.of(DefaultModelTypes.ITEM_MODEL_GENERATOR, null);
        if(model instanceof BlockModel){
            ModelInstance<?> modelInstance = ((BlockModelExtension)model).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.CUBOID, (BlockModel)model);
                ((BlockModelExtension)model).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
