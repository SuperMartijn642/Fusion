package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private final BlockStateModel original;
    private final List<BlockStateModel> models;
    private final boolean showBreakingOverlay;

    public BlockModelModifierBakedModel(BlockStateModel original, List<BlockStateModel> models, boolean showBreakingOverlay){
        this.original = original;
        this.models = new ArrayList<>(models.size() + 1);
        this.models.add(original);
        this.models.addAll(models);
        this.showBreakingOverlay = showBreakingOverlay;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            this.original.collectParts(level, pos, state, random, parts);
            return;
        }
        this.models.forEach(model -> model.collectParts(level, pos, state, random, parts));
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random){
        List<Object> keys = new ArrayList<>(this.models.size() + 2);
        keys.add(this);
        keys.add(this.original.createGeometryKey(blockView, pos, state, random));
        for(BlockStateModel model : this.models)
            keys.add(model.createGeometryKey(blockView, pos, state, random));
        return keys;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            //noinspection deprecation
            this.original.collectParts(random, parts);
            return;
        }
        //noinspection deprecation
        this.models.forEach(model -> model.collectParts(random, parts));
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.original.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.original.particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.original.materialFlags();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return this.original.materialFlags(level, pos, state);
    }
}
