package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private static final ModelProperty<Long> SEED_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<ModelData[]> DATA_PROPERTY = new ModelProperty<>();
    private static final ModelProperty<BlockState> STATE_PROPERTY = new ModelProperty<>();

    private final BlockStateModel original;
    private final List<BlockStateModel> models;
    private final boolean showBreakingOverlay;
    private final boolean isOriginalSimpleModel;
    private final boolean hasSimpleModels, hasNonSimpleModels;
    private final List<BlockStateModel> nonSimpleModels;
    private final int materialFlags;

    public BlockModelModifierBakedModel(BlockStateModel original, List<BlockStateModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
        List<BlockStateModel> nonSimpleModels = new ArrayList<>();
        for(BlockStateModel model : this.models){
            if(!model.getClass().equals(SingleVariant.class))
                nonSimpleModels.add(model);
        }
        this.isOriginalSimpleModel = original.getClass().equals(SingleVariant.class);
        this.hasSimpleModels = nonSimpleModels.size() < this.models.size();
        this.hasNonSimpleModels = !nonSimpleModels.isEmpty();
        this.nonSimpleModels = nonSimpleModels.isEmpty() ? null : List.copyOf(nonSimpleModels);

        int materialFlags = 0;
        for(BlockStateModel model : this.models)
            materialFlags |= model.materialFlags();
        this.materialFlags = materialFlags;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData data){
        // Get model data properties
        Long seed = data.get(SEED_PROPERTY);
        ModelData[] arr = data.get(DATA_PROPERTY);
        BlockState state = data.get(STATE_PROPERTY);
        // When rendering breaking overlay, only submit the original model
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            if(this.isOriginalSimpleModel){
                this.original.collectParts(random, parts, ModelData.EMPTY);
                return;
            }
            ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
            if(seed != null)
                random.setSeed(seed);
            this.original.collectParts(random, parts, subData);
            return;
        }
        // If there's only simple models, use vanilla method
        if(!this.hasNonSimpleModels){
            this.collectParts(random, parts);
            return;
        }
        // Submit all models
        int i = 0;
        for(BlockStateModel model : this.models){
            if(model.getClass().equals(SingleVariant.class)) // For simple models, don't increase the model data array index
                model.collectParts(random, parts, ModelData.EMPTY);
            else{ // For complex models, get the correct model data
                ModelData subData = arr == null || arr[i] == null ? ModelData.EMPTY : arr[i];
                if(seed != null)
                    random.setSeed(seed);
                model.collectParts(random, parts, subData);
                i++;
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            this.original.collectParts(random, parts, ModelData.EMPTY);
            return;
        }
        this.models.forEach(model -> model.collectParts(random, parts, ModelData.EMPTY));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        // If there's only simple models, no need for model data
        if(!this.hasNonSimpleModels)
            return ModelData.EMPTY;
        // Add seed and block state
        ModelData.Builder builder = ModelData.builder()
            .with(SEED_PROPERTY, state.getSeed(pos))
            .with(STATE_PROPERTY, state);
        // Gather model data for complex models
        ModelData[] arr = new ModelData[this.nonSimpleModels.size()];
        for(int i = 0; i < this.nonSimpleModels.size(); i++)
            arr[i] = this.nonSimpleModels.get(i).getModelData(level, pos, state, data);
        return builder.with(DATA_PROPERTY, arr).build();
    }

    @Override
    public Material.Baked particleMaterial(@NotNull ModelData data){
        if(this.isOriginalSimpleModel)
            return this.original.particleMaterial(ModelData.EMPTY);
        // Get appropriate model data
        ModelData[] arr = data.get(DATA_PROPERTY);
        ModelData subData = arr == null || arr[0] == null ? ModelData.EMPTY : arr[0];
        return this.original.particleMaterial(subData);
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.original.particleMaterial(ModelData.EMPTY);
    }

    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }
}
