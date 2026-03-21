package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
        this.hasSimpleModels = nonSimpleModels.size() < this.models.size();
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData data, @Nullable RenderType renderType){
        Long seed = data.get(SEED_PROPERTY);
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(seed != null)
                random.setSeed(seed);
            this.original.collectParts(random, parts, data, renderType);
            return;
        }
        if(!this.hasNonSimpleModels){
            for(BlockStateModel model : this.models){
                if(seed != null)
                    random.setSeed(seed);
                model.collectParts(random, parts, ModelData.EMPTY, renderType);
            }
            return;
        }

        ModelData[] arr = data.get(DATA_PROPERTY);
        BlockState state = data.get(STATE_PROPERTY);
        Boolean isDefaultRenderType = null;
        int i = 0;
        for(BlockStateModel model : this.models){
            if(model.getClass().equals(SingleVariant.class)){
                if(isDefaultRenderType == null)
                    //noinspection removal
                    isDefaultRenderType = renderType == null || state == null || ItemBlockRenderTypes.getRenderLayers(state).contains(renderType);
                if(isDefaultRenderType)
                    //noinspection deprecation
                    model.collectParts(random, parts);
            }else{
                ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
                if(state == null || model.getRenderTypes(state, random, subData).contains(renderType)){
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
            this.original.collectParts(random, parts);
            return;
        }
        this.models.forEach(model -> model.collectParts(random, parts, ModelData.EMPTY, null));
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        Long seed = data.get(SEED_PROPERTY);
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(seed != null)
                rand.setSeed(seed);
            return this.original.getRenderTypes(state, rand, data);
        }
        if(!this.hasNonSimpleModels)
            return BlockStateModel.super.getRenderTypes(state, rand, data);
        ChunkRenderTypeSet renderTypes = this.hasSimpleModels ? BlockStateModel.super.getRenderTypes(state, rand, data) : ChunkRenderTypeSet.none();
        ModelData[] arr = data.get(DATA_PROPERTY);
        for(int i = 0; i < this.nonSimpleModels.size(); i++){
            ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
            if(seed != null)
                rand.setSeed(seed);
            renderTypes = ChunkRenderTypeSet.union(this.nonSimpleModels.get(i).getRenderTypes(state, rand, subData), renderTypes);
        }
        return renderTypes;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        ModelData.Builder builder = ModelData.builder()
            .with(SEED_PROPERTY, state.getSeed(pos));
        if(!this.hasNonSimpleModels)
            return builder.build();
        ModelData[] arr = new ModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return builder.with(DATA_PROPERTY, arr).with(STATE_PROPERTY, state).build();
    }

    @Override
    public TextureAtlasSprite particleIcon(ModelData data){
        return this.original.particleIcon(data);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        //noinspection deprecation
        return this.original.particleIcon();
    }
}
