package com.supermartijn642.fusion.model.modifiers.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BakedModel {

    private static final Function<SimpleBakedModel,ChunkRenderTypeSet> getBlockRenderTypes;
    private static final Function<SimpleBakedModel,List<RenderType>> getItemRenderTypes, getFabulousItemRenderTypes;

    static{
        try{
            Field blockRenderTypes = SimpleBakedModel.class.getDeclaredField("blockRenderTypes");
            blockRenderTypes.setAccessible(true);
            getBlockRenderTypes = model -> {
                try{
                    return (ChunkRenderTypeSet)blockRenderTypes.get(model);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
            Field itemRenderTypes = SimpleBakedModel.class.getDeclaredField("itemRenderTypes");
            itemRenderTypes.setAccessible(true);
            getItemRenderTypes = model -> {
                try{
                    //noinspection unchecked
                    return (List<RenderType>)itemRenderTypes.get(model);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
            Field fabulousItemRenderTypes = SimpleBakedModel.class.getDeclaredField("fabulousItemRenderTypes");
            fabulousItemRenderTypes.setAccessible(true);
            getFabulousItemRenderTypes = model -> {
                try{
                    //noinspection unchecked
                    return (List<RenderType>)fabulousItemRenderTypes.get(model);
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }
            };
        }catch(NoSuchFieldException e){
            throw new RuntimeException("Fusion failed to make vanilla model render types accessible!", e);
        }
    }

    private static final ModelProperty<Long> SEED_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<ModelData[]> DATA_PROPERTY = new ModelProperty<>();

    private final BakedModel original;
    private final List<BakedModel> models;
    private final boolean showBreakingOverlay;
    private final boolean isOriginalSimpleModel;
    private final boolean hasNonSimpleModels;
    private final List<BakedModel> nonSimpleModels;
    private final List<BakedQuad> quads;
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] culledQuads = new List[6];
    private final ChunkRenderTypeSet chunkRenderTypes;
    private final List<RenderType> itemRenderTypes, fabulousItemRenderTypes;
    private final boolean addNativeBlockRenderTypes, addNativeItemRenderTypes, addNativeFabulousItemRenderTypes;

    public BlockModelModifierBakedModel(BakedModel original, List<BakedModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
        List<BakedModel> nonSimpleModels = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        //noinspection unchecked
        List<BakedQuad>[] culledQuads = IntStream.range(0, 6).mapToObj(i -> new ArrayList<>()).toArray(List[]::new);
        ChunkRenderTypeSet chunkRenderTypes = ChunkRenderTypeSet.none();
        Set<RenderType> itemRenderTypes = new HashSet<>(), fabulousItemRenderTypes = new HashSet<>();
        boolean addNativeBlockRenderTypes = false, addNativeItemRenderTypes = false, addNativeFabulousItemRenderTypes = false;
        RandomSource random = RandomSource.create();
        for(BakedModel model : this.models){
            if(!model.getClass().equals(SimpleBakedModel.class))
                nonSimpleModels.add(model);
            else{
                //noinspection deprecation
                quads.addAll(model.getQuads(null, null, random));
                for(Direction side : Direction.values())
                    //noinspection deprecation
                    culledQuads[side.ordinal()].addAll(model.getQuads(null, side, random));
                ChunkRenderTypeSet modelChunkRenderTypes = getBlockRenderTypes.apply((SimpleBakedModel)model);
                if(modelChunkRenderTypes == null)
                    addNativeBlockRenderTypes = true;
                else
                    chunkRenderTypes = ChunkRenderTypeSet.union(modelChunkRenderTypes, chunkRenderTypes);
                List<RenderType> modelItemRenderTypes = getItemRenderTypes.apply((SimpleBakedModel)model);
                if(modelItemRenderTypes == null)
                    addNativeItemRenderTypes = true;
                else
                    itemRenderTypes.addAll(modelItemRenderTypes);
                List<RenderType> modelFabulousItemRenderTypes = getFabulousItemRenderTypes.apply((SimpleBakedModel)model);
                if(modelFabulousItemRenderTypes == null)
                    addNativeFabulousItemRenderTypes = true;
                else
                    fabulousItemRenderTypes.addAll(modelFabulousItemRenderTypes);
            }
        }
        this.isOriginalSimpleModel = original.getClass().equals(SimpleBakedModel.class);
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);
        this.quads = List.copyOf(quads);
        for(Direction side : Direction.values())
            this.culledQuads[side.ordinal()] = List.copyOf(culledQuads[side.ordinal()]);
        this.chunkRenderTypes = chunkRenderTypes;
        this.itemRenderTypes = List.copyOf(itemRenderTypes);
        this.fabulousItemRenderTypes = List.copyOf(fabulousItemRenderTypes);
        this.addNativeBlockRenderTypes = addNativeBlockRenderTypes;
        this.addNativeItemRenderTypes = addNativeItemRenderTypes;
        this.addNativeFabulousItemRenderTypes = addNativeFabulousItemRenderTypes;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType){
        // Get model data properties
        Long seed = data.get(SEED_PROPERTY);
        ModelData[] arr = data.get(DATA_PROPERTY);
        // Check whether quads from simple models should be submitted
        boolean addSimpleQuads = renderType == null
            || this.chunkRenderTypes.contains(renderType)
            || (this.addNativeBlockRenderTypes && BakedModel.super.getRenderTypes(state, random, data).contains(renderType));
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel)
                return addSimpleQuads ? this.original.getQuads(state, side, random, ModelData.EMPTY, null) : List.of();
            ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
            if(renderType == null || state == null || this.original.getRenderTypes(state, random, subData).contains(renderType)){
                if(seed != null)
                    random.setSeed(seed);
                return this.original.getQuads(state, side, random, subData, renderType);
            }
            return List.of();
        }
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        // Start with quads from simple models
        List<BakedQuad> quads = addSimpleQuads ? new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]) : new ArrayList<>();
        // Gather quads from complex models
        for(int i = 0; i < this.nonSimpleModels.size(); i++){
            BakedModel model = this.nonSimpleModels.get(i);
            ModelData modelData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
            if(renderType == null || state == null || model.getRenderTypes(state, random, modelData).contains(renderType)){
                if(seed != null)
                    random.setSeed(seed);
                quads.addAll(model.getQuads(state, side, random, modelData, renderType));
            }
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getQuads(state, side, random);
        if(!this.hasNonSimpleModels)
            return side == null ? this.quads : this.culledQuads[side.ordinal()];
        List<BakedQuad> quads = new ArrayList<>(side == null ? this.quads : this.culledQuads[side.ordinal()]);
        for(BakedModel model : this.nonSimpleModels)
            quads.addAll(model.getQuads(state, side, random));
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data){
        // Get model data properties
        Long seed = data.get(SEED_PROPERTY);
        ModelData[] arr = data.get(DATA_PROPERTY);
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel)
                return this.original.getRenderTypes(state, random, ModelData.EMPTY);
            ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
            if(seed != null)
                random.setSeed(seed);
            return this.original.getRenderTypes(state, random, subData);
        }
        // Start with the render types from simple models
        ChunkRenderTypeSet renderTypes = this.chunkRenderTypes;
        if(this.addNativeBlockRenderTypes)
            renderTypes = ChunkRenderTypeSet.union(renderTypes, BakedModel.super.getRenderTypes(state, random, ModelData.EMPTY));
        // Gather render types from complex models
        for(int i = 0; i < this.nonSimpleModels.size(); i++){
            BakedModel model = this.nonSimpleModels.get(i);
            ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
            if(seed != null)
                random.setSeed(seed);
            renderTypes = ChunkRenderTypeSet.union(renderTypes, model.getRenderTypes(state, random, subData));
        }
        return renderTypes;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous){
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getRenderTypes(stack, fabulous);
        // If no special models or native render types, just return the cached list
        if((fabulous ? !this.addNativeFabulousItemRenderTypes : !this.addNativeItemRenderTypes) && !this.hasNonSimpleModels)
            return fabulous ? this.fabulousItemRenderTypes : this.itemRenderTypes;
        // Collect all render types
        Set<RenderType> renderTypes = new HashSet<>(5);
        renderTypes.addAll(this.itemRenderTypes);
        if(fabulous ? this.addNativeFabulousItemRenderTypes : this.addNativeItemRenderTypes)
            renderTypes.addAll(BakedModel.super.getRenderTypes(stack, true));
        if(this.hasNonSimpleModels){
            for(BakedModel model : this.nonSimpleModels)
                renderTypes.addAll(model.getRenderTypes(stack, true));
        }
        return new ArrayList<>(renderTypes);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous){
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getRenderPasses(stack, fabulous);
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return this.models;
        // Collect passes from the submodels
        List<BakedModel> passes = new ArrayList<>(this.models.size());
        for(BakedModel model : this.models)
            passes.addAll(model.getRenderPasses(stack, fabulous));
        return passes;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        // If there's only simple models, no need for model data
        if(!this.hasNonSimpleModels)
            return ModelData.EMPTY;
        // Add seed and block state
        ModelData.Builder builder = ModelData.builder()
            .with(SEED_PROPERTY, state.getSeed(pos));
        // Gather model data for complex models
        ModelData[] arr = new ModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return builder.with(DATA_PROPERTY, arr).build();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.original.getTransforms();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer(){
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides(){
        return this.original.getOverrides();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType){
        return this.original.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        return this.original.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data){
        if(this.isOriginalSimpleModel)
            return this.original.getParticleIcon(ModelData.EMPTY);
        // Get appropriate model data
        ModelData[] arr = data.get(DATA_PROPERTY);
        ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
        return this.original.getParticleIcon(subData);
    }
}
