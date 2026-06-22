package com.supermartijn642.fusion.model;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class FusionBlockModelData {

    public static final ThreadLocal<Identifier> CURRENT_MODEL = new ThreadLocal<>();
    public static final ThreadLocal<Pair<BlockModelPart,ResolvedModel>> MISSING_MODEL = new ThreadLocal<>();
    public static Pair<SpriteLoader.Preparations,SpriteLoader.Preparations> blockItemAtlasSprites;

    @Nullable
    public static FusionBlockModelData get(UnbakedModel model){
        return model instanceof BlockModel ? ((BlockModelExtension)model).getFusionData() : null;
    }

    private final BlockModel cuboidModel;
    private final Identifier identifier;
    private final UntypedModelInstance model;

    private UnbakedGeometry geometry;
    private boolean resolvedGeometry;
    private ItemTransforms itemTransforms;
    private boolean resolvedItemTransforms;
    private TextureSlots.Data textureSlots;
    private Identifier parent;
    private boolean resolvedParent = false;

    public FusionBlockModelData(Identifier identifier, UntypedModelInstance model){
        this.cuboidModel = new BlockModel(null, null, null, null, new TextureSlots.Data(Map.of()), null);
        //noinspection DataFlowIssue
        ((BlockModelExtension)(Object)this.cuboidModel).setFusionData(this);
        this.identifier = identifier;
        this.model = model;
    }

    public FusionBlockModelData(ModelInstance<?> model){
        this(Optional.ofNullable(CURRENT_MODEL.get()).orElseGet(() -> IdentifierUtil.withFusionNamespace("unknown")), model);
    }

    public BlockModel asBlockModel(){
        return this.cuboidModel;
    }

    public BlockStateModel bakeBlockModel(ResolvedModel wrapper, ModelBaker modelBakery, ModelState modelState){
        // Store missing models
        MISSING_MODEL.set(Pair.of(modelBakery.missingBlockModelPart(), modelBakery.getModel(ModelResolver.MISSING_MODEL)));

        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        BlockStateModelBakingContext context = new BlockStateModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.of(modelState),
            modelBakery,
            wrapper.getTopAdditionalProperties()
        );
        // Let the custom model handle the actual baking
        BlockStateModel model;
        try{
            model = this.model.bakeBlockStateModel(context, ModelStack.empty().push(this.model, this.identifier));
        }catch(Exception e){
            if(this.model instanceof ModelInstance<?>)
                throw new RuntimeException("Encountered an exception while baking block model of type '" + ModelTypeRegistryImpl.getIdentifier(((ModelInstance<?>)this.model).getModelType()) + "' for  '" + this.identifier + "'!", e);
            else
                throw new RuntimeException("Encountered an exception while baking untyped block model for '" + this.identifier + "'!", e);
        }
        if(model == null)
            model = context.getMissingBlockStateModel();
        // Log warnings
        if(!warnings.isEmpty())
            LoggingHelper.logUserWarnings(warnings, "Warnings for block model '%s':", this.identifier);

        // Clear missing models
        MISSING_MODEL.remove();
        return model;
    }

    public ItemModel bakeItemModel(ResolvedModel wrapper, List<ItemTintSource> tintSources, ModelBaker modelBakery, EntityModelSet entityModelSet){
        // Store missing models
        MISSING_MODEL.set(Pair.of(modelBakery.missingBlockModelPart(), modelBakery.getModel(ModelResolver.MISSING_MODEL)));

        // Collect warnings
        List<String> warnings = new ArrayList<>();
        // Create baking context
        ItemModelBakingContext context = new ItemModelBakingContextImpl(
            warnings::add,
            this.identifier,
            ModelTransform.identity(),
            modelBakery,
            wrapper.getTopAdditionalProperties(),
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
            LoggingHelper.logUserWarnings(warnings, "Warnings for item model '%s':", this.identifier);

        // Clear missing models
        MISSING_MODEL.remove();
        return model;
    }

    public UnbakedGeometry geometry(){
        if(!this.resolvedGeometry){
            ModelGeometry geometry = this.model.getGeometry();
            if(geometry != null){
                this.geometry = (textureSlots, modelBaker, modelState, name) -> {
                    return geometry.bake(ModelTransform.of(modelState), (key, required) -> {
                        Material material = textureSlots.getMaterial(key);
                        if(material == null){
                            if(required)
                                modelBaker.sprites().reportMissingReference(key, name);
                            return modelBaker.sprites().get(ModelMaterial.missingBlockOrItemAtlas().toMaterial(), () -> {throw new AssertionError("Failed to bake missing sprite!");});
                        }
                        return modelBaker.sprites().get(material, name);
                    }).toQuadCollection();
                };
            }
            this.resolvedGeometry = true;
        }
        return this.geometry;
    }

    public UnbakedModel.GuiLight guiLight(){
        return this.model.getGuiLight();
    }

    public Boolean ambientOcclusion(){
        return this.model.getAmbientOcclusion();
    }

    public ItemTransforms transforms(){
        if(!this.resolvedItemTransforms){
            ImmutableMap.Builder<ItemDisplayContext,ItemTransform> builder = ImmutableMap.builder();
            for(ItemDisplayContext type : ItemDisplayContext.values()){
                ItemTransform transform = this.model.getItemTransform(type);
                if(transform != null)
                    builder.put(type, transform);
            }
            ImmutableMap<ItemDisplayContext,ItemTransform> transforms = builder.build();
            if(!transforms.isEmpty()){
                this.itemTransforms = new ItemTransforms(
                    transforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                    transforms.getOrDefault(ItemDisplayContext.ON_SHELF, ItemTransform.NO_TRANSFORM),
                    transforms
                );
            }
            this.resolvedItemTransforms = true;
        }
        return this.itemTransforms;
    }

    public TextureSlots.Data textureSlots(){
        if(this.textureSlots == null){
            TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
            this.model.getMaterials().forEach((key, value) -> {
                if(value.isLeft())
                    textures.addReference(key, value.left());
                else
                    textures.addTexture(key, value.right().toMaterial());
            });
            this.textureSlots = textures.build();
        }
        return this.textureSlots;
    }

    public Identifier parent(){
        if(!this.resolvedParent){
            this.parent = this.model.getParents().stream().filter(Either::isLeft).map(p -> p.left()).findFirst().orElse(null);
            this.resolvedParent = true;
        }
        return this.parent;
    }

    public static boolean containsFusionModelsOrTextures(ResolvedModel wrapper){
        // Check if the wrapper contains a Fusion model
        if(get(wrapper.wrapped()) != null)
            return true;
        // Check if the model has Fusion textures
        Predicate<Identifier> isFusionTexture = isFusionTexture();
        TextureSlots.Data textureSlots = wrapper.wrapped().textureSlots();
        for(Map.Entry<String,TextureSlots.SlotContents> entry : textureSlots.values().entrySet()){
            if(entry.getValue() instanceof TextureSlots.Value(Material material) && isFusionTexture.test(material.texture()))
                return true;
        }
        // Check parent
        ResolvedModel parent = wrapper.parent();
        if(parent != null)
            return containsFusionModelsOrTextures(parent);
        return false;
    }

    private static Predicate<Identifier> isFusionTexture(){
        Pair<SpriteLoader.Preparations,SpriteLoader.Preparations> sprites = blockItemAtlasSprites;
        SpriteLoader.Preparations blockSprites = sprites.left();
        SpriteLoader.Preparations itemSprites = sprites.right();
        return identifier -> {
            TextureAtlasSprite sprite = itemSprites.getSprite(identifier);
            if(sprite == null)
                sprite = blockSprites.getSprite(identifier);
            return sprite != null && SpriteHelper.getSpriteInstance(sprite) != null;
        };
    }

    public static UntypedModelInstance getModelInstance(UnbakedModel model){
        FusionBlockModelData fusionData = get(model);
        if(fusionData != null)
            return fusionData.model;
        if(model instanceof BlockModel){
            //noinspection DataFlowIssue
            ModelInstance<?> modelInstance = ((BlockModelExtension)model).getFusionModel();
            if(modelInstance == null){
                modelInstance = new ModelInstanceImpl<>(DefaultModelTypes.CUBOID, (BlockModel)model);
                //noinspection DataFlowIssue
                ((BlockModelExtension)model).setFusionModel(modelInstance);
            }
            return modelInstance;
        }
        if(model.getClass().equals(ItemModelGenerator.class))
            return ModelInstance.of(DefaultModelTypes.ITEM_MODEL_GENERATOR, (ItemModelGenerator)model);
        return new ModelInstanceImpl<>(DefaultModelTypes.UNKNOWN, model);
    }
}
