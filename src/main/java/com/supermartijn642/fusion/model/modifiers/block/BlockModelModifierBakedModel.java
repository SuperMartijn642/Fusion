package com.supermartijn642.fusion.model.modifiers.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
    private static final Function<SimpleBakedModel,RenderType> getItemRenderType;

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
            Field itemRenderTypes = SimpleBakedModel.class.getDeclaredField("itemRenderType");
            itemRenderTypes.setAccessible(true);
            getItemRenderType = model -> {
                try{
                    return (RenderType)itemRenderTypes.get(model);
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
    private final RenderType itemRenderType;
    private final boolean addNativeBlockRenderTypes, checkNativeItemRenderType;

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
        Set<RenderType> itemRenderTypes = new HashSet<>();
        boolean addNativeBlockRenderTypes = false, checkNativeItemRenderType = false;
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
                RenderType modelItemRenderType = getItemRenderType.apply((SimpleBakedModel)model);
                if(modelItemRenderType == null)
                    checkNativeItemRenderType = true;
                else
                    itemRenderTypes.add(modelItemRenderType);
            }
        }
        this.isOriginalSimpleModel = original.getClass().equals(SimpleBakedModel.class);
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);
        this.quads = List.copyOf(quads);
        for(Direction side : Direction.values())
            this.culledQuads[side.ordinal()] = List.copyOf(culledQuads[side.ordinal()]);
        this.chunkRenderTypes = chunkRenderTypes;
        this.itemRenderType = itemRenderTypes.size() == 1 ? itemRenderTypes.iterator().next()
            : itemRenderTypes.contains(Sheets.translucentItemSheet()) ? Sheets.translucentItemSheet()
            : itemRenderTypes.contains(Sheets.cutoutBlockSheet()) ? Sheets.cutoutBlockSheet()
            : itemRenderTypes.contains(Sheets.solidBlockSheet()) ? Sheets.solidBlockSheet()
            : Sheets.translucentItemSheet();
        this.addNativeBlockRenderTypes = addNativeBlockRenderTypes;
        this.checkNativeItemRenderType = checkNativeItemRenderType && this.itemRenderType != Sheets.translucentItemSheet();
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
    public RenderType getRenderType(ItemStack stack){
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getRenderType(stack);
        // If any model requires the translucent render type, use that
        if(this.itemRenderType == Sheets.translucentItemSheet())
            return Sheets.translucentItemSheet();
        if(this.checkNativeItemRenderType && BakedModel.super.getRenderType(stack) == Sheets.translucentItemSheet())
            return Sheets.translucentItemSheet();
        if(this.hasNonSimpleModels){
            for(BakedModel model : this.nonSimpleModels){
                if(model.getRenderType(stack) == Sheets.translucentItemSheet())
                    return Sheets.translucentItemSheet();
            }
        }
        return this.itemRenderType;
    }

    @SuppressWarnings("removal")
    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack){
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null)
            return this.original.getRenderPasses(stack);
        // If there's only simple models, return the cached quads
        if(!this.hasNonSimpleModels)
            return this.models;
        // Collect passes from the submodels
        List<BakedModel> passes = new ArrayList<>(this.models.size());
        for(BakedModel model : this.models)
            passes.addAll(model.getRenderPasses(stack));
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
    public TextureAtlasSprite getParticleIcon(){
        return this.original.getParticleIcon();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType){
        return this.original.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public void applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform){
        this.original.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data){
        return this.original.getParticleIcon(data);
    }
}
