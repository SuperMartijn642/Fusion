package com.supermartijn642.fusion.model;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    private static final ModelProperty<ModelData[]> SUB_MODEL_DATA = new ModelProperty<>();

    protected abstract List<BlockStateModel> getModels();

    protected ModelData getModelData(int modelIndex, BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData){
        return model.getModelData(level, pos, state, modelData);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        List<BlockStateModel> models = this.getModels();
        ModelData[] subModelData = new ModelData[models.size()];
        for(int i = 0; i < models.size(); i++)
            subModelData[i] = models.get(i).getModelData(level, pos, state, modelData);
        return ModelData.builder().with(SUB_MODEL_DATA, subModelData).build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData modelData){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        List<BlockStateModel> models = this.getModels();
        long seed = random.nextLong();
        for(int i = 0; i < models.size(); i++){
            random.setSeed(seed);
            models.get(i).collectParts(random, parts, subModelData == null ? ModelData.EMPTY : subModelData[i]);
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        long seed = random.nextLong();
        for(BlockStateModel model : this.getModels()){
            random.setSeed(seed);
            model.collectParts(random, parts);
        }
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.getModels().getFirst().particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        int materialFlags = 0;
        for(BlockStateModel model : this.getModels())
            materialFlags |= model.materialFlags();
        return materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag){
        for(BlockStateModel model : this.getModels()){
            if(model.hasMaterialFlag(flag))
                return true;
        }
        return false;
    }

    @Override
    public Material.Baked particleMaterial(@NotNull ModelData modelData){
        ModelData[] subModelData = modelData.get(SUB_MODEL_DATA);
        return this.getModels().getFirst().particleMaterial(subModelData == null ? ModelData.EMPTY : subModelData[0]);
    }
}
