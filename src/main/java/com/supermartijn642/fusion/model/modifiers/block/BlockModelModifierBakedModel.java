package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private static final ModelProperty<Long> SEED_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<ModelData[]> DATA_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<BlockState> STATE_PROPERTY = new ModelProperty<>();

    private final BlockStateModel original;
    private final List<BlockStateModel> models;
    private final boolean showBreakingOverlay;
    private final boolean isOriginalSimpleModel;
    private final boolean hasSimpleModels, hasNonSimpleModels;
    private final List<BlockStateModel> nonSimpleModels;

    public BlockModelModifierBakedModel(BlockStateModel original, List<BlockStateModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
        List<BlockStateModel> nonSimpleModels = new ArrayList<>();
        for(BlockStateModel model : this.models){
            if(!model.getClass().equals(SingleVariant.class))
                nonSimpleModels.add(model);
        }
        this.isOriginalSimpleModel = original.getClass().equals(SingleVariant.class);
        this.hasSimpleModels = nonSimpleModels.size() < this.models.size();
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData data, @Nullable ChunkSectionLayer renderType){
        // Get model data properties
        Long seed = data.get(SEED_PROPERTY);
        ModelData[] arr = data.get(DATA_PROPERTY);
        BlockState state = data.get(STATE_PROPERTY);
        // Keep track of whether the given render type is part of the block's default render types
        Boolean isDefaultRenderType = null;
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel){
                this.original.collectParts(random, parts, ModelData.EMPTY, renderType);
                return;
            }
            ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
            if(renderType == null || state == null || this.original.getRenderTypes(state, random, subData).contains(renderType)){
                if(seed != null)
                    random.setSeed(seed);
                this.original.collectParts(random, parts, subData, renderType);
            }
            return;
        }
        // If there's only simple models, use vanilla method
        if(!this.hasNonSimpleModels){
            this.collectParts(random, parts);
            return;
        }
        // Submit all models
        int i = 0;
        for(BlockStateModel model : this.models){
            if(model.getClass().equals(SingleVariant.class)){ // For simple models, don't increase the model data array index
                if(isDefaultRenderType == null)
                    //noinspection deprecation
                    isDefaultRenderType = renderType == null || state == null || ItemBlockRenderTypes.getRenderLayers(state).contains(renderType);
                if(isDefaultRenderType)
                    //noinspection deprecation
                    model.collectParts(random, parts);
            }else{ // For complex models, get the correct model data
                ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
                if(renderType == null || state == null || model.getRenderTypes(state, random, subData).contains(renderType)){
                    if(seed != null)
                        random.setSeed(seed);
                    model.collectParts(random, parts, subData, renderType);
                }
                i++;
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            this.original.collectParts(random, parts, ModelData.EMPTY, null);
            return;
        }
        this.models.forEach(model -> model.collectParts(random, parts, ModelData.EMPTY, null));
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        // Start with block's default render types
        Collection<ChunkSectionLayer> renderTypes = this.hasSimpleModels ? BlockStateModel.super.getRenderTypes(state, rand, ModelData.EMPTY) : List.of();
        boolean isMutableSet = false;
        // Get model data properties
        ModelData[] arr = data.get(DATA_PROPERTY);
        Long seed = data.get(SEED_PROPERTY);
        // When rendering breaking overlay, only submit the original model's render types
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel)
                return renderTypes;
            ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
            if(seed != null)
                rand.setSeed(seed);
            return this.original.getRenderTypes(state, rand, subData);
        }
        // If there's only simple models, just return the block's default render types
        if(!this.hasNonSimpleModels)
            return BlockStateModel.super.getRenderTypes(state, rand, ModelData.EMPTY);
        // Gather render types from complex models
        for(int i = 0; i < this.nonSimpleModels.size(); i++){
            ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
            if(!isMutableSet){
                renderTypes = renderTypes.isEmpty() ? EnumSet.noneOf(ChunkSectionLayer.class) : EnumSet.copyOf(renderTypes);
                isMutableSet = true;
            }
            if(seed != null)
                rand.setSeed(seed);
            renderTypes.addAll(this.nonSimpleModels.get(i).getRenderTypes(state, rand, subData));
        }
        return renderTypes;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        // If there's only simple models, no need for model data
        if(!this.hasNonSimpleModels)
            return ModelData.EMPTY;
        // Add seed and block state
        ModelData.Builder builder = ModelData.builder()
            .with(SEED_PROPERTY, state.getSeed(pos))
            .with(STATE_PROPERTY, state);
        // Gather model data for complex models
        ModelData[] arr = new ModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return builder.with(DATA_PROPERTY, arr).build();
    }

    @Override
    public TextureAtlasSprite particleIcon(ModelData data){
        if(this.isOriginalSimpleModel)
            return this.original.particleIcon(ModelData.EMPTY);
        // Get appropriate model data
        ModelData[] arr = data.get(DATA_PROPERTY);
        ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
        return this.original.particleIcon(subData);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.original.particleIcon(ModelData.EMPTY);
    }
}
