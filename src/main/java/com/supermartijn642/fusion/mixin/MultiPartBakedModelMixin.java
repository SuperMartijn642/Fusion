package com.supermartijn642.fusion.mixin;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created 04/07/2023 by SuperMartijn642
 */
@Mixin(value = MultipartBakedModel.class, priority = 900)
public class MultiPartBakedModelMixin implements IForgeBakedModel {

    @Unique
    private static final ModelProperty<Map<IBakedModel,IModelData>> SUB_MODEL_DATA = new ModelProperty<>();

    @Final
    @Shadow
    private List<Pair<Predicate<BlockState>,IBakedModel>> selectors;
    @Final
    @Shadow
    private Map<BlockState,BitSet> selectorCache;

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull ILightReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData modelData){
        if(state == null)
            return IForgeBakedModel.super.getModelData(level, pos, state, modelData);

        // Combine the model data from all the sub-models, so it doesn't get lost
        BitSet bitSet = this.selectorCache.get(state);
        if(bitSet == null){
            bitSet = new BitSet();
            for(int i = 0; i < this.selectors.size(); ++i){
                Pair<Predicate<BlockState>,IBakedModel> pair = this.selectors.get(i);
                if(pair.getLeft().test(state))
                    bitSet.set(i);
            }
            this.selectorCache.put(state, bitSet);
        }
        Map<IBakedModel,IModelData> subModelData = IntStream.range(0, this.selectors.size())
            .filter(bitSet::get)
            .mapToObj(this.selectors::get)
            .map(Pair::getRight)
            .collect(Collectors.toMap(
                Function.identity(),
                model -> model.getModelData(level, pos, state, modelData),
                (first, second) -> first
            ));

        // Put it into model data
        ModelDataMap.Builder builder = new ModelDataMap.Builder();
        builder.withInitial(SUB_MODEL_DATA, subModelData);
        return builder.build();
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData modelData){
        if(state == null)
            return Collections.emptyList();

        BitSet bitSet = this.selectorCache.get(state);
        if(bitSet == null){
            bitSet = new BitSet();
            for(int i = 0; i < this.selectors.size(); ++i){
                Pair<Predicate<BlockState>,IBakedModel> pair = this.selectors.get(i);
                if(pair.getLeft().test(state))
                    bitSet.set(i);
            }
            this.selectorCache.put(state, bitSet);
        }

        List<BakedQuad> list = Lists.newArrayList();
        long seed = random.nextLong();
        Map<IBakedModel,IModelData> subModelData = modelData.hasProperty(SUB_MODEL_DATA) ? modelData.getData(SUB_MODEL_DATA) : Collections.emptyMap();
        for(int j = 0; j < bitSet.length(); ++j){
            if(bitSet.get(j)){
                IBakedModel model = this.selectors.get(j).getRight();
                IModelData data = subModelData.get(model);
                list.addAll(model.getQuads(state, side, new Random(seed), data == null ? EmptyModelData.INSTANCE : data));
            }
        }

        return list;
    }
}
