package com.supermartijn642.fusion.mixin;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

/**
 * Created 26/10/2023 by SuperMartijn642
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements IBakedModelExtension {

    @Final
    @Shadow
    private int totalWeight;
    @Final
    @Shadow
    private List<WeightedEntry.Wrapper<BakedModel>> list;
    @Unique
    private final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        if(state == null)
            return modelData;

        // Get the seed for the given block position
        RandomSource randomSource = this.RANDOM.get();
        randomSource.setSeed(state.getSeed(pos));
        // Update the model data for the selected sub model
        BakedModel model = WeightedRandom.getWeightedItem(this.list, Math.abs((int)randomSource.nextLong()) % this.totalWeight)
            .map(WeightedEntry.Wrapper::getData).orElse(null);
        return model == null ? modelData : model.getModelData(level, pos, state, modelData);
    }
}
