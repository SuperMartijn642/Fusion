package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
    public static FusionBlockModelData get(UnbakedModel model){
        return model instanceof FusionBlockModelData ? (FusionBlockModelData)model : null;
    }

    private final ResourceLocation identifier;
    private final UntypedModelInstance model;
    private boolean resolved = false;
    private List<UnbakedModel> parents = List.of();
    private Map<ResourceLocation,UntypedModelInstance> dependencies;

    private FusionBlockModelData(ResourceLocation identifier, UntypedModelInstance model){
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
    public Collection<ResourceLocation> getDependencies(){
        return this.model.getDependencies();
    }

    public static Collection<Material> gatherBlockModelMaterials(BlockModel model,
                                                                 Function<ResourceLocation,UnbakedModel> modelGetter,
                                                                 Set<com.mojang.datafixers.util.Pair<String,String>> missingMaterials){
        BlockModel topModel = model;
        // Check whether any model in the parent chain is a fusion model
        while(model != null && !(model instanceof FusionBlockModelData) && model.parentLocation != null){
            UnbakedModel parent = modelGetter.apply(model.parentLocation);
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

    private static Map<ResourceLocation,UnbakedModel> resolveAndGatherMaterials(UnbakedModel model,
                                                                                Function<ResourceLocation,UnbakedModel> modelGetter,
                                                                                @Nullable Set<com.mojang.datafixers.util.Pair<String,String>> missingMaterials,
                                                                                @Nullable Set<Material> materials,
                                                                                Set<UnbakedModel> modelStack){
        // Check for loops
        if(!modelStack.add(model))
            FusionClient.LOGGER.error(
                "Found circular dependency chain while resolving model '{}': {} -> '{}'",
                ((BlockModel)modelStack.stream().findFirst().orElseThrow()).name,
                modelStack.stream().map(m -> m instanceof BlockModel ? "'" + ((BlockModel)m).name + "'" : "'unknown'").collect(Collectors.joining(" -> ")),
                model instanceof BlockModel ? "'" + ((BlockModel)model).name + "'" : "'unknown'"
            );

        Map<ResourceLocation,UnbakedModel> dependencies = new HashMap<>();
        Set<String> requiredMaterialKeys = null;

        // Handle unknown models
        if(!(model instanceof BlockModel)){
            // Track resolved models
            Function<ResourceLocation,UnbakedModel> trackingResolver = new Function<>() {
                @Override
                public UnbakedModel apply(ResourceLocation identifier){
                    UnbakedModel model = modelGetter.apply(identifier);
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
                List<BlockElement> elements;
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
                    for(BlockElement element : elements){
                        for(BlockElementFace face : element.faces.values()){
                            requiredMaterialKeys.add(face.texture);
                        }
                    }
                }
            }
            // Resolve parent model
            if(((BlockModel)model).parentLocation != null){
                if(((BlockModel)model).parent == null){
                    UnbakedModel parent = modelGetter.apply(((BlockModel)model).parentLocation);
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
                UnbakedModel dependencyModel = modelGetter.apply(dependency);
                if(dependencyModel == null){
                    FusionClient.LOGGER.error("Missing dependency model '{}' for model '{}'!", dependency, ((FusionBlockModelData)model).identifier);
                    dependencyModel = modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION);
                }
                dependencies.put(dependency, dependencyModel);
                dependencies.putAll(resolveAndGatherMaterials(dependencyModel, modelGetter, null, null, modelStack));
            }
            for(UnbakedModel parent : ((FusionBlockModelData)model).parents){
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
                for(UnbakedModel m : modelStack){
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
                    ((BlockModel)modelStack.stream().findFirst().orElseThrow()).name
                )),
                keys -> FusionClient.LOGGER.error("Found circular material chain ({}) for model '{}'!", keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")), ((BlockModel)modelStack.stream().findFirst().orElseThrow()).name)
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

    public void resolve(Function<ResourceLocation,UnbakedModel> resolver){
        if(this.resolved)
            return;
        this.resolved = true;

        // Parents
        List<com.supermartijn642.fusion.api.util.Either<ResourceLocation,UntypedModelInstance>> parents = this.model.getParents();
        this.parents = new ArrayList<>(parents.size());
        for(com.supermartijn642.fusion.api.util.Either<ResourceLocation,UntypedModelInstance> parent : parents){
            UnbakedModel parentModel;
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
        this.parents = List.copyOf(this.parents);

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

    private void setDependencies(Map<ResourceLocation,UnbakedModel> dependencies){
        ImmutableMap.Builder<ResourceLocation,UntypedModelInstance> builder = ImmutableMap.builderWithExpectedSize(dependencies.size());
        dependencies.forEach((key, model) -> builder.put(key, getModelInstance(model)));
        this.dependencies = builder.build();
    }

    @Override
    public BakedModel bake(ModelBakery modelBakery, Function<Material,TextureAtlasSprite> textureGetter, ModelState modelState, ResourceLocation topModelIdentifier){
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
        BakedModel bakedModel;
        try{
            bakedModel = this.model.bakeModel(context, ModelStack.empty().push(this.model, this.identifier));
        }catch(Exception e){
            if(this.model instanceof ModelInstance<?>)
                throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)this.model).getModelType()) + "' for  '" + this.identifier + "'!", e);
            else
                throw new RuntimeException("Encountered an exception while baking untyped block model for '" + this.identifier + "'!", e);
        }
        if(bakedModel == null)
            bakedModel = context.getMissingBakedModel();
        // Log warnings
        if(!warnings.isEmpty())
            LoggingHelper.logUserWarnings(warnings, "Warnings for block model '{}':", this.identifier);
        return bakedModel;
    }

    @Contract("_,_,_,_,!null -> !null")
    private static <T> T getFromModelTree(UnbakedModel model,
                                          Function<FusionBlockModelData,@Nullable T> fusionModelGetter,
                                          Function<UntypedModelInstance,@Nullable T> unknownModelGetter,
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
                                          Function<UntypedModelInstance,@Nullable T> unknownModelGetter,
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
            m -> m.model.getAmbientOcclusion(),
            UntypedModelInstance::getAmbientOcclusion,
            true
        );
    }

    @Override
    public GuiLight getGuiLight(){
        return getFromModelTree(
            this,
            m -> m.guiLight,
            UntypedModelInstance::getGuiLight,
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
                    face.lightEmission() == null ? 0 : face.lightEmission(),
                    face.ambientOcclusion() == null || face.ambientOcclusion()
                ));
            }
            return new BlockElement(
                element.from(), element.to(),
                faces,
                element.rotation(),
                element.shade() == null || element.shade()
            );
        }).toList();
    }

    private static Map<String,Either<Material,String>> getMaterials(UntypedModelInstance model){
        Map<String,Either<Material,String>> materials = new HashMap<>();
        model.getMaterials().forEach((key, value) -> {
            if(value.isLeft())
                materials.put(key, Either.right(value.left()));
            else
                materials.put(key, Either.left(value.right().toMaterial()));
        });
        return Map.copyOf(materials);
    }

    private static ItemTransforms getItemTransforms(UntypedModelInstance model){
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

    public static UntypedModelInstance getModelInstance(UnbakedModel model){
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
