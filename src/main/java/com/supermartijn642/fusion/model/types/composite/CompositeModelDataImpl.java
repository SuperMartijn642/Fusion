package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.model.types.composite.CompositeModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeModelDataImpl implements CompositeModelData {

    public static CompositeModelData.ModelEntry entry(ResourceLocation model, ModelTransform transform, @Nullable ModelPredicate condition){
        return new ModelEntryImpl(Either.left(model), transform, condition);
    }

    public static CompositeModelData.ModelEntry entry(ModelInstance<?> model, ModelTransform transform, @Nullable ModelPredicate condition){
        return new ModelEntryImpl(Either.right(model), transform, condition);
    }

    private final List<List<ModelEntry>> models;
    private final BaseModelData baseModelData;

    CompositeModelDataImpl(List<List<ModelEntry>> models, BaseModelData baseModelData){
        this.models = models;
        this.baseModelData = baseModelData;
    }

    @Override
    public List<List<ModelEntry>> getModels(){
        return this.models;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(){
        return this.baseModelData.getMaterials();
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return this.baseModelData.getAmbientOcclusion();
    }

    @Override
    public @Nullable BlockModel.GuiLight getGuiLight(){
        return this.baseModelData.getGuiLight();
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type){
        return this.baseModelData.getItemTransform(type);
    }

    @Override
    public List<ItemOverride> getItemOverrides(){
        return this.baseModelData.getItemOverrides();
    }

    @Override
    public @Nullable Boolean getShade(){
        return this.baseModelData.getShade();
    }

    @Override
    public @Nullable Boolean getEmissive(){
        return this.baseModelData.getEmissive();
    }

    private static class ModelEntryImpl implements CompositeModelData.ModelEntry {

        private final Either<ResourceLocation,ModelInstance<?>> model;
        private final ModelTransform transform;
        private final ModelPredicate condition;

        private ModelEntryImpl(Either<ResourceLocation,ModelInstance<?>> model, ModelTransform transform, ModelPredicate condition){
            this.model = model;
            this.transform = transform;
            this.condition = condition;
        }

        @Override
        public Either<ResourceLocation,ModelInstance<?>> getModel(){
            return this.model;
        }

        @Override
        public ModelTransform getTransform(){
            return this.transform;
        }

        @Override
        public @Nullable ModelPredicate getCondition(){
            return this.condition;
        }
    }
}
