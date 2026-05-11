package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created 26/10/2023 by SuperMartijn642
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements IForgeBakedModel, CustomRenderTypeBakedModel {

    @Final
    @Shadow
    private int totalWeight;
    @Final
    @Shadow
    private List<WeightedBakedModel.WeightedModel> list;

    @Unique
    private static final ConcurrentHashMap<Class<? extends IForgeBakedModel>,Boolean> MODELS_PRODUCING_DATA = new ConcurrentHashMap<>();

    @Unique
    private boolean fusion$innerModelProducesData;
    @Unique
    private boolean hasCustomRenderTypeModels;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void init(List<WeightedBakedModel.WeightedModel> models, CallbackInfo ci){
        this.fusion$innerModelProducesData = this.list.stream().anyMatch(w -> w.model != null && MODELS_PRODUCING_DATA.computeIfAbsent(w.model.getClass(), clz -> {
            try{
                Method method = clz.getMethod("getModelData", ILightReader.class, BlockPos.class, BlockState.class, IModelData.class);
                return method.getDeclaringClass() != IForgeBakedModel.class;
            }catch(NoSuchMethodException e){
                // This should not happen, but if so, assume it does produce data
                return true;
            }
        }));
        for(WeightedBakedModel.WeightedModel entry : this.list){
            if(entry.model instanceof CustomRenderTypeBakedModel){
                this.hasCustomRenderTypeModels = true;
                break;
            }
        }
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random random, @Nonnull IModelData modelData){
        return (WeightedRandom.getWeightedItem(this.list, Math.abs((int)random.nextLong()) % this.totalWeight)).model.getQuads(state, side, random, modelData);
    }

    @Override
    public @Nonnull IModelData getModelData(@Nonnull ILightReader level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData modelData){
        // Skip expensive computations below if none of the inner models need model data
        if(state == null || !this.fusion$innerModelProducesData)
            return modelData;

        // Get the seed for the given block position
        Random randomSource = new Random(state.getSeed(pos));
        // Update the model data for the selected sub model
        WeightedBakedModel.WeightedModel entry = WeightedRandom.getWeightedItem(this.list, Math.abs((int)randomSource.nextLong()) % this.totalWeight);
        IBakedModel model = entry == null ? null : entry.model;
        return model == null ? modelData : model.getModelData(level, pos, state, modelData);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
        if(!this.hasCustomRenderTypeModels)
            return isDefaultRenderType;
        for(WeightedBakedModel.WeightedModel entry : this.list){
            IBakedModel model = entry.model;
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                return true;
        }
        return false;
    }
}
