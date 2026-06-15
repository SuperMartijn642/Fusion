package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
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

import java.util.*;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBlockStateModel implements BlockStateModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final BlockStateModel defaultModel;
    private final List<ConditionalList> entries;
    private final int initialCapacityGuess;

    public CompositeBlockStateModel(BlockStateModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;

        // Resolve default values
        int guaranteedModelCount = 0;
        for(ConditionalList list : entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate == null || entry.predicate.alwaysTrue())
                    guaranteedModelCount++;
            }
        }
        this.initialCapacityGuess = Math.min(guaranteedModelCount * 2, entries.size());
    }

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable ModelData modelData){
        boolean canCollectModelData = level != null && pos != null && state != null;
        if(modelData == null)
            modelData = ModelData.EMPTY;
        List<BlockStateModel> models = new ArrayList<>(this.initialCapacityGuess);
        List<ModelData> subModelData = canCollectModelData ? new ArrayList<>(this.initialCapacityGuess) : null;
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model == null)
                continue;
            models.add(model);
            if(canCollectModelData)
                subModelData.add(model.getModelData(level, pos, state, modelData));
        }
        return new RenderData(state, models, subModelData, canCollectModelData ? null : modelData);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state, modelData))
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> output, ModelData modelData, @Nullable ChunkSectionLayer renderType){
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null, modelData);
        long seed = random.nextLong();
        boolean shouldCheckRenderType = renderData.state != null && renderType != null;
        for(int i = 0; i < renderData.models.size(); i++){
            BlockStateModel model = renderData.models.get(i);
            ModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(i);
            random.setSeed(seed);
            if(!shouldCheckRenderType || model.getRenderTypes(renderData.state, random, data).contains(renderType))
                model.collectParts(random, output, data, renderType);
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(null, null, null);
            if(model != null){
                random.setSeed(seed);
                model.collectParts(random, parts);
            }
        }
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData modelData){
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, state, modelData);
        Set<ChunkSectionLayer> renderTypes = EnumSet.noneOf(ChunkSectionLayer.class);
        for(int i = 0; i < renderData.models.size(); i++){
            BlockStateModel model = renderData.models.get(i);
            ModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(i);
            renderTypes.addAll(model.getRenderTypes(state, rand, data));
        }
        return renderTypes;
    }

    @Override
    public TextureAtlasSprite particleIcon(@NotNull ModelData modelData){
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            return this.defaultModel.particleIcon(modelData);
        if(renderData.models.isEmpty())
            return this.defaultModel.particleIcon(renderData.generalModelData);
        BlockStateModel model = renderData.models.get(0);
        ModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(0);
        return model.particleIcon(data);
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.defaultModel.particleIcon();
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        BlockStateModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BlockStateModel model, ModelPredicate predicate) {
    }

    private record RenderData(BlockState state, List<BlockStateModel> models, @Nullable List<ModelData> modelData, ModelData generalModelData) {
    }
}
