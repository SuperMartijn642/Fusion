package com.supermartijn642.fusion.model;

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

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public abstract class CombinedBlockStateModel implements BlockStateModel {

    public static BlockStateModel of(List<BlockStateModel> models){
        return new CombinedBlockStateModel() {
            @Override
            protected List<BlockStateModel> getModels(){
                return models;
            }
        };
    }

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

    private static RenderData getRenderData(ModelData modelData){
        RenderData renderData = modelData.get(RENDER_DATA);
        return renderData == null ? RenderData.EMPTY : renderData;
    }

    protected abstract List<BlockStateModel> getModels();

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        List<BlockStateModel> models = this.getModels();
        ModelData[] subModelData = new ModelData[models.size()];
        for(int i = 0; i < models.size(); i++)
            subModelData[i] = models.get(i).getModelData(level, pos, state, modelData);
        return ModelData.builder().with(RENDER_DATA, new RenderData(state, subModelData)).build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData modelData, ChunkSectionLayer renderType){
        RenderData renderData = getRenderData(modelData);
        ModelData[] subModelData = renderData.subModelData;
        BlockState state = renderData.state;

        // Check whether we need to check the models' render types against the given one
        boolean doRenderTypeCheck = renderType != null && state != null;

        List<BlockStateModel> models = this.getModels();
        long seed = random.nextLong();
        for(int i = 0; i < models.size(); i++){
            random.setSeed(seed);
            BlockStateModel model = models.get(i);
            ModelData subData = subModelData == null ? ModelData.EMPTY : subModelData[i];
            if(!doRenderTypeCheck || model.getRenderTypes(state, random, subData).contains(renderType)){
                random.setSeed(seed);
                model.collectParts(random, parts, subData, renderType);
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            model.collectParts(random, parts);
        }
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.getModels().getFirst().particleIcon();
    }

    @Override
    public TextureAtlasSprite particleIcon(@NotNull ModelData modelData){
        RenderData renderData = getRenderData(modelData);
        ModelData[] subModelData = renderData.subModelData;
        return this.getModels().getFirst().particleIcon(subModelData == null ? ModelData.EMPTY : subModelData[0]);
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData modelData){
        RenderData renderData = getRenderData(modelData);
        ModelData[] subModelData = renderData.subModelData;
        List<BlockStateModel> models = this.getModels();
        Set<ChunkSectionLayer> renderTypes = EnumSet.noneOf(ChunkSectionLayer.class);
        long seed = random.nextLong();
        for(int i = 0; i < models.size(); i++){
            random.setSeed(seed);
            renderTypes.addAll(models.get(i).getRenderTypes(state, random, subModelData == null ? ModelData.EMPTY : subModelData[i]));
        }
        return renderTypes;
    }

    private record RenderData(BlockState state, ModelData[] subModelData) {
        static final RenderData EMPTY = new RenderData(null, new ModelData[0]);
    }
}
