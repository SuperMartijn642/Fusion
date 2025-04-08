package com.supermartijn642.fusion.mixin;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.MultipartModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * TODO: this mixin should be removed once <a href="https://github.com/MinecraftForge/MinecraftForge/issues/10523">MinecraftForge#10523</a> is fixed
 * <p>
 * Created 04/07/2023 by SuperMartijn642
 */
@Mixin(value = MultiPartBakedModel.class, priority = 900)
public class MultiPartBakedModelMixin implements IForgeBakedModel {

    @Final
    @Shadow
    private List<Pair<Predicate<BlockState>,BakedModel>> selectors;

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        if(state == null)
            return modelData;

        // Combine the model data from all the sub-models, so it doesn't get lost
        MultipartModelData.Builder builder = null;
        //noinspection DataFlowIssue
        BitSet bitSet = ((MultiPartBakedModel)(Object)this).getSelectors(state);
        for(int i = 0; i < bitSet.length(); ++i){
            if(bitSet.get(i)){
                BakedModel model = this.selectors.get(i).getRight();
                ModelData subData = model.getModelData(level, pos, state, modelData);
                if(subData != modelData){
                    if(builder == null)
                        builder = MultipartModelData.builder();
                    builder.with(model, subData);
                }
            }
        }
        return builder == null ? modelData : ModelData.builder().with(MultipartModelData.PROPERTY, builder.build()).build();
    }
}
