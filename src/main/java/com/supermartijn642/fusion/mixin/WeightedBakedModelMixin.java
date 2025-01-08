package com.supermartijn642.fusion.mixin;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    private List<WeightedEntry.Wrapper<BakedModel>> list;

    @Unique
    private static final ConcurrentHashMap<Class<? extends IForgeBakedModel>, Boolean> MODELS_PRODUCING_DATA = new ConcurrentHashMap<>();

    @Unique
    private boolean fusion$innerModelProducesData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.fusion$innerModelProducesData = list.stream().anyMatch(w -> w.getData() != null && MODELS_PRODUCING_DATA.computeIfAbsent(w.getData().getClass(), clz -> {
            try {
                var method = clz.getMethod("getModelData", BlockAndTintGetter.class, BlockPos.class, BlockState.class, ModelData.class);
                return method.getDeclaringClass() != IForgeBakedModel.class;
            } catch(NoSuchMethodException e) {
                // This should not happen, but if so, assume it does produce data
                return true;
            }
        }));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        // Skip expensive computations below if none of the inner models need model data
        if(state == null || !fusion$innerModelProducesData)
            return modelData;

        // Get the seed for the given block position
        RandomSource randomSource = new SingleThreadedRandomSource(state.getSeed(pos));
        // Update the model data for the selected sub model
        BakedModel model = WeightedRandom.getWeightedItem(this.list, Math.abs((int)randomSource.nextLong()) % this.totalWeight)
            .map(WeightedEntry.Wrapper::getData).orElse(null);
        return model == null ? modelData : model.getModelData(level, pos, state, modelData);
    }
}
