package com.supermartijn642.fusion.mixin;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBlockStateModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: this mixin should be removed once <a href="https://github.com/MinecraftForge/MinecraftForge/issues/10523">MinecraftForge#10523</a> is fixed
 * <p>
 * Created 04/07/2023 by SuperMartijn642
 */
@Mixin(value = MultiPartModel.class, priority = 900)
public class MultiPartModelMixin implements IForgeBlockStateModel {

    @Unique
    private static final ModelProperty<Map<BlockStateModel,ModelData>> SUB_MODEL_DATA_PROPERTY = new ModelProperty<>();

    @Final
    @Shadow
    private MultiPartModel.SharedBakedState shared;
    @Final
    @Shadow
    private BlockState blockState;
    @Shadow
    private List<BlockStateModel> models;

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData modelData, ChunkSectionLayer renderType){
        if(this.models == null)
            this.models = this.shared.selectModels(this.blockState);

        Map<BlockStateModel,ModelData> subModelData = modelData.get(SUB_MODEL_DATA_PROPERTY);
        if(subModelData == null)
            subModelData = Map.of();
        long i = random.nextLong();
        for(BlockStateModel model : this.models){
            random.setSeed(i);
            ModelData subData = subModelData.getOrDefault(model, ModelData.EMPTY);
            model.collectParts(random, parts, subData, renderType);
        }
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        if(this.models == null)
            this.models = this.shared.selectModels(this.blockState);

        // Combine the model data from all the sub-models, so it doesn't get lost
        Map<BlockStateModel,ModelData> subModelData = null;
        for(BlockStateModel model : this.models){
            ModelData subData = model.getModelData(level, pos, this.blockState, modelData);
            if(subData != modelData){
                if(subModelData == null)
                    subModelData = new IdentityHashMap<>();
                subModelData.put(model, subData);
            }
        }
        return subModelData == null ? modelData : ModelData.builder().with(SUB_MODEL_DATA_PROPERTY, subModelData).build();
    }
}
