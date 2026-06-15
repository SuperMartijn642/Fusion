package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBlockStateModel implements BlockStateModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private final BlockStateModel defaultModel;
    private final List<ConditionalList> entries;
    private final int initialCapacityGuess;
    private final int materialFlags;

    public CompositeBlockStateModel(BlockStateModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;

        // Resolve default values
        int guaranteedModelCount = 0;
        int materialFlags = 0;
        for(ConditionalList list : entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate == null || entry.predicate.alwaysTrue())
                    guaranteedModelCount++;
                materialFlags |= entry.model.materialFlags();
            }
        }
        this.initialCapacityGuess = Math.min(guaranteedModelCount * 2, entries.size());
        this.materialFlags = materialFlags;
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
        return new RenderData(models, subModelData, canCollectModelData ? null : modelData);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state, modelData))
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output, ModelData modelData){
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null, modelData);
        long seed = random.nextLong();
        for(int i = 0; i < renderData.models.size(); i++){
            random.setSeed(seed);
            BlockStateModel model = renderData.models.get(i);
            ModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(i);
            model.collectParts(random, output, data);
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
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
    public Material.Baked particleMaterial(@NotNull ModelData modelData){
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            return this.defaultModel.particleMaterial(modelData);
        if(renderData.models.isEmpty())
            return this.defaultModel.particleMaterial(renderData.generalModelData);
        BlockStateModel model = renderData.models.get(0);
        ModelData data = renderData.modelData == null ? renderData.generalModelData : renderData.modelData.get(0);
        return model.particleMaterial(data);
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.defaultModel.particleMaterial();
    }

    @Override
    public int materialFlags(){
        return this.materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(int flag){
        return (this.materialFlags & flag) != 0;
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

    private record RenderData(List<BlockStateModel> models, @Nullable List<ModelData> modelData, ModelData generalModelData) {
    }
}
