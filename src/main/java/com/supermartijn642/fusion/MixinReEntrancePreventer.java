package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Created 03/06/2026 by SuperMartijn642
 */
public class MixinReEntrancePreventer {

    public static Collection<ResourceLocation> fusionBlockModelData$gatherBlockModelMaterials(BlockModel model, Function<ResourceLocation,IUnbakedModel> modelGetter, Set<String> missingMaterials){
        return FusionBlockModelData.gatherBlockModelMaterials(model, modelGetter, missingMaterials);
    }

    public static ThreadLocal<ResourceLocation> fusionBlockModel$CURRENT_MODEL(){
        return FusionBlockModelData.CURRENT_MODEL;
    }

    public static void FusionBlockModelData$modelBakery(WeakReference<ModelBakery> value){
        FusionBlockModelData.modelBakery = value;
    }

    public static void modelBakeryMixin$getBakedModel(ResourceLocation location, ISprite modelState, VertexFormat vertexFormat, CallbackInfoReturnable<IBakedModel> ci, ModelBakery modelBakery, IUnbakedModel unbakedModel, AtlasTexture blockAtlas, Map<Triple<ResourceLocation,IModelState,Boolean>,IBakedModel> bakedCache){
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel, blockAtlas)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                FusionBlockModelData.gatherBlockModelMaterials(fusionData, l -> {
                    IUnbakedModel m = modelBakery.unbakedCache.get(l);
                    return m == null ? modelBakery.unbakedCache.get(l) : m;
                }, new LinkedHashSet<>());
            }
            IBakedModel baked = fusionData.bake(modelBakery, blockAtlas::getSprite, modelState, vertexFormat);
            // Add baked model to the cache
            Triple<ResourceLocation,IModelState,Boolean> key = Triple.of(location, modelState.getState(), modelState.isUvLocked());
            bakedCache.put(key, baked);
            ci.setReturnValue(baked);
        }
    }

    public static void forgeBlockModelRendererMixin$collectModelsByRandomOffset(VertexLighterFlat lighter, IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci, ThreadLocal<ModelsByRandomOffset> MODELS_BY_RANDOM_OFFSET){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = MODELS_BY_RANDOM_OFFSET.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    ((VertexLighterFlatExtension)lighter).setFusionRandomOffsetOverwrite(entry.getOffset());
                    if(ForgeBlockModelRenderer.render(lighter, level, entry, state, pos, buffer, cull, random, seed, modelData))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ((VertexLighterFlatExtension)lighter).setFusionRandomOffsetOverwrite(null);
        }
        ci.setReturnValue(rendered.get());
    }

    public static void modelBlockRendererMixin$collectModelsByRandomOffsetWithAO(BlockModelRenderer blockModelRenderer, IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci, ThreadLocal<ModelsByRandomOffset> modelsByRandomOffsetLocal){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = modelsByRandomOffsetLocal.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    if(blockModelRenderer.renderModelSmooth(level, entry, state, pos, buffer, cull, random, seed, modelData))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        ci.setReturnValue(rendered.get());
    }

    public static void modelBlockRendererMixin$collectModelsByRandomOffsetWithoutAO(BlockModelRenderer blockModelRenderer, IEnviromentBlockReader level, IBakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull, Random random, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci, ThreadLocal<ModelsByRandomOffset> modelsByRandomOffsetLocal){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        ModelsByRandomOffset modelsByRandomOffset = modelsByRandomOffsetLocal.get();
        AtomicBoolean rendered = new AtomicBoolean(false);
        modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(modelsByRandomOffset, level, pos, state);
            modelsByRandomOffset.foreach(
                entry -> {
                    ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.set(entry.getOffset());
                    if(blockModelRenderer.renderModelFlat(level, entry, state, pos, buffer, cull, random, seed, modelData))
                        rendered.set(true);
                }
            );
        }finally{
            modelsByRandomOffset.reset();
            ModelsByRandomOffset.RANDOM_OFFSET_OVERWRITE.remove();
        }
        ci.setReturnValue(rendered.get());
    }
}
