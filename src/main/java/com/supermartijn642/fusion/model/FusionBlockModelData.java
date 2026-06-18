package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModelData extends ModelBlock implements IModel {

    public static final ThreadLocal<ResourceLocation> CURRENT_MODEL = new ThreadLocal<>();

    @Nullable
    public static FusionBlockModelData get(ModelBlock model){
        return model instanceof FusionBlockModelData ? (FusionBlockModelData)model : null;
    }

    private final ResourceLocation identifier;
    private final UntypedModelInstance model;
    private boolean uvLock = false;
    private boolean resolved = false;
    private List<ModelBlock> parents = Collections.emptyList();
    private Map<ResourceLocation,UntypedModelInstance> dependencies;

    public FusionBlockModelData(ResourceLocation identifier, UntypedModelInstance model){
        super(null, Collections.emptyList(), Collections.emptyMap(), true, true, ItemCameraTransforms.DEFAULT, Collections.emptyList());
        this.identifier = identifier;
        this.model = model;
    }

    public FusionBlockModelData(ModelInstance<?> model){
        super(null, Collections.emptyList(), Collections.emptyMap(), true, true, ItemCameraTransforms.DEFAULT, Collections.emptyList());
        ResourceLocation name = CURRENT_MODEL.get();
        this.identifier = name == null ? IdentifierUtil.withFusionNamespace("unknown") : name;
        this.model = model;
    }

    @Override
    public Optional<ModelBlock> asVanillaModel(){
        return Optional.of(this);
    }

    @Override
    public IModel uvlock(boolean value){
        FusionBlockModelData copy = new FusionBlockModelData(this.identifier, this.model);
        copy.uvLock = value;
        copy.resolved = this.resolved;
        copy.parents = this.parents;
        copy.dependencies = this.dependencies;
        return copy;
    }

    @Override
    public Collection<ResourceLocation> getTextures(){
        this.resolve();
        List<ResourceLocation> textures = new ArrayList<>();
        for(String value : this.textures.values()){
            if(IdentifierUtil.isValidIdentifier(value))
                textures.add(new ResourceLocation(value));
        }
        return textures;
    }

    @Override
    public Collection<ResourceLocation> getDependencies(){
        this.resolve();
        List<ItemOverride> itemOverrides = this.model.getItemOverrides();
        if(itemOverrides.isEmpty())
            return this.model.getDependencies();
        Set<ResourceLocation> dependencies = new HashSet<>(this.model.getDependencies());
        for(ItemOverride override : itemOverrides)
            dependencies.add(override.getLocation());
        return dependencies;
    }

    public void resolve(){
        if(this.resolved)
            return;
        this.resolved = true;

        // Dependencies
        Collection<ResourceLocation> dependencies = this.model.getDependencies();
        this.dependencies = new HashMap<>(dependencies.size());
        this.dependencies.put(this.identifier, this.model);
        this.collectDependencies(this.model);

        // Parents
        List<com.supermartijn642.fusion.api.util.Either<ResourceLocation,UntypedModelInstance>> parents = this.model.getParents();
        this.parents = new ArrayList<>(parents.size());
        for(com.supermartijn642.fusion.api.util.Either<ResourceLocation,UntypedModelInstance> parent : parents){
            if(parent.isLeft()){
                IModel parentModel = ModelLoaderRegistry.getModelOrMissing(parent.left());
                if(parentModel == ModelLoaderRegistry.getMissingModel())
                    FusionClient.LOGGER.error("Missing parent model '{}' for model '{}'!", parent.left(), this.identifier);
                this.dependencies.put(parent.left(), getModelInstance(parentModel));
                parentModel.asVanillaModel().ifPresent(this.parents::add);
                this.collectDependencies(getModelInstance(parentModel));
            }else{
                this.parents.add(new FusionBlockModelData(IdentifierUtil.withSuffix(this.identifier, "_parent"), parent.right()));
                this.collectDependencies(parent.right());
            }
        }
        this.parents = ImmutableList.copyOf(this.parents);

        // Fill vanilla block model properties
        this.parentLocation = parents.isEmpty() ? null : parents.get(0).leftOrNull();
        if(!this.parents.isEmpty())
            this.parent = this.parents.get(0);
        this.elements = FusionBlockModelData.getElements(this.model);
        if(this.elements == null)
            this.elements = Collections.emptyList();
        this.gui3d = this.model.getIsGui3d() == null || this.model.getIsGui3d();
        this.ambientOcclusion = this.model.getAmbientOcclusion() == null || this.model.getAmbientOcclusion();
        this.cameraTransforms = FusionBlockModelData.getItemCameraTransforms(this.model);
        this.textures = FusionBlockModelData.getMaterials(this.model);
    }

    private void collectDependencies(UntypedModelInstance model){
        for(ResourceLocation dependency : model.getDependencies()){
            UntypedModelInstance dependencyModel = FusionBlockModelData.getModelInstance(ModelLoaderRegistry.getModelOrMissing(dependency));
            this.dependencies.put(dependency, dependencyModel);
            this.collectDependencies(dependencyModel);
        }
    }

    @Override
    public IBakedModel bake(IModelState modelState, VertexFormat format, Function<ResourceLocation,TextureAtlasSprite> textureGetter){
        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        ModelBakingContext context = new ModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.of(modelState, this.uvLock),
            textureGetter,
            this.dependencies
        );
        // Let the custom model handle the actual baking
        IBakedModel bakedModel;
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
            LoggingHelper.logUserWarnings(warnings, "Warnings for block model '%s':", this.identifier);
        return bakedModel;
    }

    @Contract("_,_,_,_,!null -> !null")
    private static <T> T getFromModelTree(ModelBlock model,
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
            return model.parent == null ? null : getFromModelTree(model.parent, fusionModelGetter, unknownModelGetter, merger, defaultValue);
        T mergedValue = null;
        for(ModelBlock parent : ((FusionBlockModelData)model).parents){
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

    private static <T> T getFromModelTree(ModelBlock model,
                                          Function<FusionBlockModelData,@Nullable T> fusionModelGetter,
                                          Function<UntypedModelInstance,@Nullable T> unknownModelGetter,
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
    public boolean isAmbientOcclusion(){
        return getFromModelTree(
            this,
            m -> m.model.getAmbientOcclusion(),
            UntypedModelInstance::getAmbientOcclusion,
            true
        );
    }

    @Override
    public boolean isGui3d(){
        return getFromModelTree(
            this,
            m -> m.model.getIsGui3d(),
            UntypedModelInstance::getIsGui3d,
            true
        );
    }

    @Override
    public ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType type){
        return getFromModelTree(
            this,
            m -> m.cameraTransforms.hasCustomTransform(type) ? m.cameraTransforms.getTransform(type) : null,
            m -> m.getItemTransform(type),
            ItemTransformVec3f.DEFAULT
        );
    }

    @Override
    public String resolveTextureName(String key, ModelBlock.Bookkeep bookkeep){
        if(!isTextureReference(key))
            return key;

        if(this == bookkeep.modelExt){
            LOGGER.warn("Unable to resolve texture due to upward reference: {} in {}", key, this.name);
            return "missingno";
        }

        String value = this.textures.get(key.substring(1));
        if(value == null){
            for(ModelBlock parent : this.parents){
                value = parent.resolveTextureName(key, bookkeep);
                if(!"missingno".equals(value))
                    break;
            }
        }

        bookkeep.modelExt = this;
        return value != null && isTextureReference(key) ?
            bookkeep.model.resolveTextureName(value, bookkeep) :
            "missingno";
    }

    private static boolean isTextureReference(String value){
        return !value.isEmpty() && value.charAt(0) == '#';
    }

    private static List<BlockPart> getElements(UntypedModelInstance model){
        ModelGeometry geometry = model.getGeometry();
        if(geometry == null)
            return null;
        if(!(geometry instanceof CuboidModelGeometry))
            return Collections.emptyList();
        return ((CuboidModelGeometry)geometry).elements().stream().map(element -> {
            Map<EnumFacing,BlockPartFace> faces = new EnumMap<>(EnumFacing.class);
            for(EnumFacing side : EnumFacing.values()){
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

    private static Map<String,String> getMaterials(UntypedModelInstance model){
        Map<String,String> materials = new HashMap<>();
        model.getMaterials().forEach((key, value) -> {
            if(value.isLeft())
                materials.put(key, value.left().isEmpty() || value.left().charAt(0) != '#' ? '#' + value.left() : value.left());
            else
                materials.put(key, value.right().texture().toString());
        });
        return ImmutableMap.copyOf(materials);
    }

    private static ItemCameraTransforms getItemCameraTransforms(UntypedModelInstance model){
        ImmutableMap.Builder<ItemCameraTransforms.TransformType,ItemTransformVec3f> transformsBuilder = ImmutableMap.builder();
        for(ItemCameraTransforms.TransformType type : ItemCameraTransforms.TransformType.values()){
            ItemTransformVec3f transform = model.getItemTransform(type);
            if(transform != null)
                transformsBuilder.put(type, transform);
        }
        ImmutableMap<ItemCameraTransforms.TransformType,ItemTransformVec3f> transforms = transformsBuilder.build();
        return new ItemCameraTransforms(
            transforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.DEFAULT),
            transforms.getOrDefault(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.DEFAULT)
        );
    }

    public static boolean containsFusionModelsOrTextures(ModelBlock model, Function<ResourceLocation,TextureAtlasSprite> spriteGetter){
        // Check if the wrapper contains a Fusion model
        if(get(model) != null)
            return true;
        // Check if the model has Fusion textures
        if(spriteGetter != null){
            for(Map.Entry<String,String> entry : model.textures.entrySet()){
                String value = entry.getValue();
                if(!value.isEmpty() && value.charAt(0) != '#' && IdentifierUtil.isValidIdentifier(value)){
                    TextureAtlasSprite sprite = spriteGetter.apply(new ResourceLocation(value));
                    if(!ModelMaterial.isMissingSprite(sprite) && SpriteHelper.getSpriteInstance(sprite) != null)
                        return true;
                }
            }
        }
        // Check parent
        ModelBlock parent = model.parent;
        if(parent != null)
            return containsFusionModelsOrTextures(parent, spriteGetter);
        return false;
    }

    private static final Class<?> VANILLA_MODEL_WRAPPER_CLASS = ModelLoader.class.getDeclaredClasses()[8];

    public static UntypedModelInstance getModelInstance(IModel model){
        if(model instanceof FusionBlockModelData)
            return ((FusionBlockModelData)model).model;
        if(model.asVanillaModel().filter(ModelBakery.MODEL_GENERATED::equals).isPresent())
            return ModelInstance.of(DefaultModelTypes.ITEM_MODEL_GENERATOR, null);
        if(VANILLA_MODEL_WRAPPER_CLASS.isInstance(model)){
            ModelBlock vanillaModel = model.asVanillaModel().orElseThrow(AssertionError::new);
            ModelInstance<?> modelInstance = ((BlockModelExtension)vanillaModel).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.CUBOID, vanillaModel);
                ((BlockModelExtension)vanillaModel).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }

    public static UntypedModelInstance getModelInstance(ModelBlock model){
        if(model instanceof FusionBlockModelData)
            return ((FusionBlockModelData)model).model;
        if(model == ModelBakery.MODEL_GENERATED)
            return ModelInstance.of(DefaultModelTypes.ITEM_MODEL_GENERATOR, null);
        ModelInstance<?> modelInstance = ((BlockModelExtension)model).getFusionModel();
        if(modelInstance == null){
            modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.CUBOID, model);
            ((BlockModelExtension)model).setFusionModel(modelInstance);
        }
        return modelInstance;
    }
}
