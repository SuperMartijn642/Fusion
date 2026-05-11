package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModelData extends BlockModel {

    public static final ThreadLocal<ResourceLocation> CURRENT_MODEL = new ThreadLocal<>();
    public static WeakReference<ModelBakery> modelBakery;

    @Nullable
    public static FusionBlockModelData get(IUnbakedModel model){
        return model instanceof FusionBlockModelData ? (FusionBlockModelData)model : null;
    }

    private final ResourceLocation identifier;
    private final ModelInstance<?> model;
    private boolean resolved = false;
    private List<IUnbakedModel> parents = Collections.emptyList();
    private Map<ResourceLocation,ModelInstance<?>> dependencies;

    private FusionBlockModelData(ResourceLocation identifier, ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), true, null, ItemCameraTransforms.NO_TRANSFORMS, Collections.emptyList());
        this.identifier = identifier;
        this.model = model;
    }

    public FusionBlockModelData(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), true, null, ItemCameraTransforms.NO_TRANSFORMS, Collections.emptyList());
        ResourceLocation name = CURRENT_MODEL.get();
        this.identifier = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
    }

    @Override
    public Collection<ResourceLocation> getDependencies(){
        return this.model.getDependencies();
    }

    public static Collection<Material> gatherBlockModelMaterials(BlockModel model,
                                                                 Function<ResourceLocation,IUnbakedModel> modelGetter,
                                                                 Set<com.mojang.datafixers.util.Pair<String,String>> missingMaterials){
        BlockModel topModel = model;
        // Check whether any model in the parent chain is a fusion model
        while(model != null && !(model instanceof FusionBlockModelData) && model.parentLocation != null){
            IUnbakedModel parent = modelGetter.apply(model.parentLocation);
            model = parent instanceof BlockModel ? (BlockModel)parent : null;
        }
        if(!(model instanceof FusionBlockModelData))
            return null;

        // Recursively gather materials and resolve Fusion model properties
        Set<Material> materials = new HashSet<>();
        resolveAndGatherMaterials(topModel, modelGetter, missingMaterials, materials, new LinkedHashSet<>());

        // Resolve particle material
        ModelGeometry.MaterialResolver.fromKeyLookup(
            key -> getFromModelTree(
                topModel,
                m -> m.model.getMaterial(key),
                m -> m.getMaterial(key),
                null
            ),
            material -> {
                materials.add(material.toMaterial());
                return null;
            },
            key -> missingMaterials.add(com.mojang.datafixers.util.Pair.of(
                "#" + key,
                topModel.name
            )),
            keys -> FusionClient.LOGGER.error("Found circular material chain ({}) for model '{}'!", keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")), topModel.name)
        ).get("particle");

        return materials;
    }

    private static Map<ResourceLocation,IUnbakedModel> resolveAndGatherMaterials(IUnbakedModel model,
                                                                                 Function<ResourceLocation,IUnbakedModel> modelGetter,
                                                                                 @Nullable Set<com.mojang.datafixers.util.Pair<String,String>> missingMaterials,
                                                                                 @Nullable Set<Material> materials,
                                                                                 Set<IUnbakedModel> modelStack){
        // Check for loops
        if(!modelStack.add(model))
            FusionClient.LOGGER.error(
                "Found circular dependency chain while resolving model '{}': {} -> '{}'",
                ((BlockModel)modelStack.stream().findFirst().orElseThrow(AssertionError::new)).name,
                modelStack.stream().map(m -> m instanceof BlockModel ? "'" + ((BlockModel)m).name + "'" : "'unknown'").collect(Collectors.joining(" -> ")),
                model instanceof BlockModel ? "'" + ((BlockModel)model).name + "'" : "'unknown'"
            );

        Map<ResourceLocation,IUnbakedModel> dependencies = new HashMap<>();
        Set<String> requiredMaterialKeys = null;

        // Handle unknown models
        if(!(model instanceof BlockModel)){
            // Track resolved models
            Function<ResourceLocation,IUnbakedModel> trackingResolver = new Function<ResourceLocation,IUnbakedModel>() {
                @Override
                public IUnbakedModel apply(ResourceLocation identifier){
                    IUnbakedModel model = modelGetter.apply(identifier);
                    if(model == null)
                        dependencies.put(identifier, modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION));
                    else
                        dependencies.put(identifier, model);
                    return model;
                }
            };
            if(materials == null || missingMaterials == null)
                model.getMaterials(trackingResolver, new LinkedHashSet<>());
            else
                materials.addAll(model.getMaterials(trackingResolver, missingMaterials));
        }else if(!(model instanceof FusionBlockModelData)){ // Handle vanilla models
            // Check whether the model has geometry
            boolean hasGeometry = false;
            if(((BlockModel)model).customData.hasCustomGeometry()){
                hasGeometry = true;
                if(materials == null || missingMaterials == null)
                    ((BlockModel)model).customData.getTextureDependencies(modelGetter, new LinkedHashSet<>());
                else
                    materials.addAll(((BlockModel)model).customData.getTextureDependencies(modelGetter, missingMaterials));
            }else{
                BlockModel parent = ((BlockModel)model).parent;
                ((BlockModel)model).parent = null;
                List<BlockPart> elements;
                try{
                    elements = ((BlockModel)model).getElements();
                }catch(Exception ignore){
                    elements = ((BlockModel)model).elements;
                }finally{
                    ((BlockModel)model).parent = parent;
                }
                if(!elements.isEmpty()){
                    hasGeometry = true;
                    requiredMaterialKeys = new LinkedHashSet<>();
                    for(BlockPart element : elements){
                        for(BlockPartFace face : element.faces.values()){
                            requiredMaterialKeys.add(face.texture);
                        }
                    }
                }
            }
            // Resolve parent model
            if(((BlockModel)model).parentLocation != null){
                if(((BlockModel)model).parent == null){
                    IUnbakedModel parent = modelGetter.apply(((BlockModel)model).parentLocation);
                    if(parent == null){
                        BlockModel.LOGGER.warn("No parent '{}' while loading model '{}'", ((BlockModel)model).parentLocation, model);
                        parent = modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION);
                        dependencies.put(((BlockModel)model).parentLocation, parent);
                        ((BlockModel)model).parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
                    }
                    if(!(parent instanceof BlockModel))
                        throw new IllegalStateException("BlockModel parent has to be a block model.");
                    ((BlockModel)model).parent = (BlockModel)parent;
                }
                if(hasGeometry)
                    dependencies.putAll(resolveAndGatherMaterials(((BlockModel)model).parent, modelGetter, null, null, modelStack));
                else
                    dependencies.putAll(resolveAndGatherMaterials(((BlockModel)model).parent, modelGetter, missingMaterials, materials, modelStack));
                dependencies.put(((BlockModel)model).parentLocation, ((BlockModel)model).parent);
            }
        }else{ // Handle Fusion models
            // Resolve model properties
            ((FusionBlockModelData)model).resolve(modelGetter);
            // Check geometry
            ModelGeometry geometry = ((FusionBlockModelData)model).model.getGeometry();
            if(geometry != null){
                requiredMaterialKeys = new LinkedHashSet<>();
                for(com.supermartijn642.fusion.api.util.Either<String,ModelMaterial> entry : geometry.getRequiredMaterials()){
                    if(entry.isLeft())
                        requiredMaterialKeys.add(entry.left());
                    else if(materials != null)
                        materials.add(entry.right().toMaterial());
                }
            }
            // Go through dependencies and parents
            for(ResourceLocation dependency : model.getDependencies()){
                if(dependencies.containsKey(dependency))
                    continue;
                IUnbakedModel dependencyModel = modelGetter.apply(dependency);
                if(dependencyModel == null){
                    FusionClient.LOGGER.error("Missing dependency model '{}' for model '{}'!", dependency, ((FusionBlockModelData)model).identifier);
                    dependencyModel = modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION);
                }
                dependencies.put(dependency, dependencyModel);
                dependencies.putAll(resolveAndGatherMaterials(dependencyModel, modelGetter, null, null, modelStack));
            }
            for(IUnbakedModel parent : ((FusionBlockModelData)model).parents){
                if(geometry == null)
                    dependencies.putAll(resolveAndGatherMaterials(parent, modelGetter, missingMaterials, materials, modelStack));
                else
                    dependencies.putAll(resolveAndGatherMaterials(parent, modelGetter, null, null, modelStack));
            }
            // Set model dependencies
            ((FusionBlockModelData)model).setDependencies(dependencies);
        }

        // Resolve material keys
        if(requiredMaterialKeys != null && materials != null && missingMaterials != null){
            // Create material resolver
            Function<String,com.supermartijn642.fusion.api.util.Either<String,ModelMaterial>> keyLookup = key -> {
                // Check model stack
                for(IUnbakedModel m : modelStack){
                    com.supermartijn642.fusion.api.util.Either<String,ModelMaterial> material = getModelInstance(m).getMaterial(key);
                    if(material != null)
                        return material;
                }
                // Check parents
                return getFromModelTree(
                    model,
                    m -> m.model.getMaterial(key),
                    m -> m.getMaterial(key),
                    null
                );
            };
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                keyLookup,
                material -> {
                    materials.add(material.toMaterial());
                    return null;
                },
                key -> missingMaterials.add(com.mojang.datafixers.util.Pair.of(
                    "#" + key,
                    ((BlockModel)modelStack.stream().findFirst().orElseThrow(AssertionError::new)).name
                )),
                keys -> FusionClient.LOGGER.error("Found circular material chain ({}) for model '{}'!", keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")), ((BlockModel)modelStack.stream().findFirst().orElseThrow(AssertionError::new)).name)
            );
            // Resolve materials
            for(String key : requiredMaterialKeys){
                if(key.startsWith("#"))
                    key = key.substring(1);
                materialResolver.get(key);
            }
        }

        // Remove model from the stack
        modelStack.remove(model);

        return dependencies;
    }

    public void resolve(Function<ResourceLocation,IUnbakedModel> resolver){
        if(this.resolved)
            return;
        this.resolved = true;

        // Parents
        List<com.supermartijn642.fusion.api.util.Either<ResourceLocation,ModelInstance<?>>> parents = this.model.getParents();
        this.parents = new ArrayList<>(parents.size());
        for(com.supermartijn642.fusion.api.util.Either<ResourceLocation,ModelInstance<?>> parent : parents){
            IUnbakedModel parentModel;
            if(parent.isLeft()){
                parentModel = resolver.apply(parent.left());
                if(parentModel == null){
                    FusionClient.LOGGER.error("Missing parent model '{}' for model '{}'!", parent.left(), this.identifier);
                    parentModel = resolver.apply(ModelBakery.MISSING_MODEL_LOCATION);
                }
            }else
                parentModel = new FusionBlockModelData(IdentifierUtil.withSuffix(this.identifier, "_parent"), parent.right());
            this.parents.add(parentModel);
        }
        this.parents = ImmutableList.copyOf(this.parents);

        // Fill vanilla block model properties
        this.parentLocation = parents.isEmpty() ? null : parents.get(0).leftOrNull();
        if(!this.parents.isEmpty() && this.parents.get(0) instanceof BlockModel)
            this.parent = (BlockModel)this.parents.get(0);
        this.elements = FusionBlockModelData.getElements(this.model);
        this.guiLight = this.model.getGuiLight();
        this.hasAmbientOcclusion = this.model.getAmbientOcclusion() == null || this.model.getAmbientOcclusion();
        this.transforms = FusionBlockModelData.getItemCameraTransforms(this.model);
        this.textureMap = FusionBlockModelData.getMaterials(this.model);
    }

    private void setDependencies(Map<ResourceLocation,IUnbakedModel> dependencies){
        ImmutableMap.Builder<ResourceLocation,ModelInstance<?>> builder = ImmutableMap.builder();
        dependencies.forEach((key, model) -> builder.put(key, getModelInstance(model)));
        this.dependencies = builder.build();
    }

    @Override
    public IBakedModel bake(ModelBakery modelBakery, Function<Material,TextureAtlasSprite> textureGetter, IModelTransform modelState, ResourceLocation topModelIdentifier){
        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        ModelBakingContext context = new ModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.of(modelState),
            textureGetter,
            this.dependencies,
            modelBakery
        );
        // Let the custom model handle the actual baking
        IBakedModel bakedModel;
        try{
            bakedModel = this.model.bakeModel(context);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(this.model.getModelType()) + "' for  '" + this.identifier + "'!", e);
        }
        // Log warnings
        if(!warnings.isEmpty()){
            FusionClient.LOGGER.warn(
                "Warnings for block model '{}':\n{}",
                this.identifier,
                warnings.stream().map(" |-> "::concat).collect(Collectors.joining("\n"))
            );
        }
        return bakedModel;
    }

    @Contract("_,_,_,_,!null -> !null")
    private static <T> T getFromModelTree(IUnbakedModel model,
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
        for(IUnbakedModel parent : ((FusionBlockModelData)model).parents){
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

    private static <T> T getFromModelTree(IUnbakedModel model,
                                          Function<FusionBlockModelData,@Nullable T> fusionModelGetter,
                                          Function<ModelInstance<?>,@Nullable T> unknownModelGetter,
                                          T defaultValue){
        return getFromModelTree(model, fusionModelGetter, unknownModelGetter, null, defaultValue);
    }

    @Override
    public List<BlockPart> getElements(){
        return getFromModelTree(
            this,
            m -> m.elements.isEmpty() ? null : m.elements,
            FusionBlockModelData::getElements,
            (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()),
            Collections.emptyList()
        );
    }

    @Override
    public boolean hasAmbientOcclusion(){
        return getFromModelTree(
            this,
            m -> m.model.getAmbientOcclusion(),
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
    public ItemCameraTransforms getTransforms(){
        return new ItemCameraTransforms(
            this.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND),
            this.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND),
            this.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND),
            this.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND),
            this.getTransform(ItemCameraTransforms.TransformType.HEAD),
            this.getTransform(ItemCameraTransforms.TransformType.GUI),
            this.getTransform(ItemCameraTransforms.TransformType.GROUND),
            this.getTransform(ItemCameraTransforms.TransformType.FIXED)
        );
    }

    @Override
    public ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType type){
        return getFromModelTree(
            this,
            m -> m.transforms.hasTransform(type) ? m.transforms.getTransform(type) : null,
            m -> m.getItemTransform(type),
            ItemTransformVec3f.NO_TRANSFORM
        );
    }

    @Override
    public Either<Material,String> findTextureEntry(String key){
        Either<Material,String> material = getFromModelTree(
            this,
            m -> m.textureMap.get(key),
            m -> {
                com.supermartijn642.fusion.api.util.Either<String,ModelMaterial> entry = m.getMaterial(key);
                return entry == null ? null : entry.flatMap(
                    Either::right,
                    mm -> Either.left(mm.toMaterial())
                );
            },
            null
        );
        if(material == null)
            return Either.left(ModelMaterial.missingBlockAtlas().toMaterial());
        return material;
    }

    private static List<BlockPart> getElements(ModelInstance<?> model){
        ModelGeometry geometry = model.getGeometry();
        if(!(geometry instanceof CuboidModelGeometry))
            return Collections.emptyList();
        return ((CuboidModelGeometry)geometry).elements().stream().map(element -> {
            Map<Direction,BlockPartFace> faces = new EnumMap<>(Direction.class);
            for(Direction side : Direction.values()){
                CuboidModelGeometry.Face face = element.face(side);
                if(face == null)
                    continue;
                String material = face.material();
                if(!material.isEmpty() && material.charAt(0) == '#')
                    material = material.substring(1);
                faces.put(side, new BlockPartFace(
                    face.cullDirection(),
                    face.tintIndex() == null ? -1 : face.tintIndex(),
                    material,
                    new BlockFaceUV(
                        face.uv() == null ? null : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                        face.rotation() == null ? 0 : face.rotation().angle()
                    )
                ));
            }
            return new BlockPart(
                element.from(), element.to(),
                faces,
                element.rotation(),
                element.shade() == null || element.shade()
            );
        }).collect(Collectors.toList());
    }

    private static Map<String,Either<Material,String>> getMaterials(ModelInstance<?> model){
        Map<String,Either<Material,String>> materials = new HashMap<>();
        model.getMaterials().forEach((key, value) -> {
            if(value.isLeft())
                materials.put(key, Either.right(value.left()));
            else
                materials.put(key, Either.left(value.right().toMaterial()));
        });
        return ImmutableMap.copyOf(materials);
    }

    private static ItemCameraTransforms getItemCameraTransforms(ModelInstance<?> model){
        ImmutableMap.Builder<ItemCameraTransforms.TransformType,ItemTransformVec3f> transformsBuilder = ImmutableMap.builder();
        for(ItemCameraTransforms.TransformType type : ItemCameraTransforms.TransformType.values()){
            ItemTransformVec3f transform = model.getItemTransform(type);
            if(transform != null)
                transformsBuilder.put(type, transform);
        }
        ImmutableMap<ItemCameraTransforms.TransformType,ItemTransformVec3f> transforms = transformsBuilder.build();
        return new ItemCameraTransforms(
            transforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.NO_TRANSFORM),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.NO_TRANSFORM)
        );
    }

    public static ModelInstance<?> getModelInstance(IUnbakedModel model){
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
