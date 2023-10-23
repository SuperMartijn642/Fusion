package com.supermartijn642.fusion.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Created 26/10/2023 by SuperMartijn642
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements IForgeBakedModel {

    @Final
    @Shadow
    private int totalWeight;
    @Final
    @Shadow
    private List<WeightedBakedModel.WeightedModel> list;
    @Unique
    private final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData modelData){
        return (WeightedRandom.getWeightedItem(this.list, Math.abs((int)random.nextLong()) % this.totalWeight)).model.getQuads(state, side, random, modelData);
    }

    @Override
    public @Nonnull IModelData getModelData(@Nonnull IEnviromentBlockReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData modelData){
        if(state == null)
            return modelData;

        // Get the seed for the given block position
        Random randomSource = this.RANDOM.get();
        randomSource.setSeed(state.getSeed(pos));
        // Update the model data for the selected sub model
        WeightedBakedModel.WeightedModel entry = WeightedRandom.getWeightedItem(this.list, Math.abs((int)randomSource.nextLong()) % this.totalWeight);
        IBakedModel model = entry == null ? null : entry.model;
        return model == null ? modelData : model.getModelData(level, pos, state, modelData);
    }
}
