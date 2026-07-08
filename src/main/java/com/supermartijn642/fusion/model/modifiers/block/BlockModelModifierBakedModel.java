package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private final BlockStateModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;
    private final Material.Baked particleMaterial;
    private final int materialFlags;

    BlockModelModifierBakedModel(BlockStateModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;

        // Resolve particle material
        Material.Baked particleMaterial = null;
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(null, null, null)){
                particleMaterial = override.model.particleMaterial();
                break;
            }
        }
        if(particleMaterial == null)
            particleMaterial = this.original.particleMaterial();
        this.particleMaterial = particleMaterial;

        // Resolve material flags
        int materialFlags = original.materialFlags();
        for(ConditionalModel model : defaultModelOverrides)
            materialFlags |= model.model.materialFlags();
        for(List<ConditionalModel> conditionals : appendModels){
            for(ConditionalModel model : conditionals){
                materialFlags |= model.model.materialFlags();
            }
        }
        this.materialFlags = materialFlags;
    }

    public void collectByOffset(ModelsByRandomOffset output, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        output.add(override.randomOffset, override.model);
                    break overrides;
                }
            }
            output.add(RandomOffsetFunction.MATCH_BLOCK, this.original);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay)
                        output.add(conditional.randomOffset, conditional.model);
                    break;
                }
            }
        }
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        long seed = random.nextLong();

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        override.model.emitQuads(emitter, level, pos, state, random, cullTest);
                    break overrides;
                }
            }
            random.setSeed(seed);
            this.original.emitQuads(emitter, level, pos, state, random, cullTest);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay){
                        random.setSeed(seed);
                        conditional.model.emitQuads(emitter, level, pos, state, random, cullTest);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        long seed = random.nextLong();

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(null, null, null)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        override.model.collectParts(random, parts);
                    break overrides;
                }
            }
            random.setSeed(seed);
            this.original.collectParts(random, parts);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(null, null, null)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay){
                        random.setSeed(seed);
                        conditional.model.collectParts(random, parts);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        // Collect keys for all models
        List<Object> keys = new ArrayList<>(this.appendModels.size() + 2);
        keys.add(this);

        long seed = random.nextLong();
        random.setSeed(seed);

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(override.conditions != null)
                        keys.add(true);
                    Object key = override.model.createGeometryKey(level, pos, state, random);
                    if(key == null)
                        return null;
                    keys.add(key);
                    break overrides;
                }
                keys.add(false);
            }
            Object key = this.original.createGeometryKey(level, pos, state, random);
            if(key == null)
                return null;
            keys.add(key);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(conditional.conditions != null)
                        keys.add(true);
                    random.setSeed(seed);
                    Object key = conditional.model.createGeometryKey(level, pos, state, random);
                    if(key == null)
                        return null;
                    keys.add(key);
                    break;
                }
                keys.add(false);
            }
        }
        return keys;
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state){
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(level, pos, state))
                return override.model.particleMaterial(level, pos, state);
        }
        return this.original.particleMaterial(level, pos, state);
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        // Collect material flags from all the models
        int materialFlags = 0;

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        materialFlags |= override.model.materialFlags(level, pos, state, random);
                    break overrides;
                }
            }
            materialFlags |= this.original.materialFlags(level, pos, state, random);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay)
                        materialFlags |= conditional.model.materialFlags(level, pos, state, random);
                    break;
                }
            }
        }
        return materialFlags;
    }

    record ConditionalModel(BlockStateModel model, @Nullable BlockStateModelPredicate conditions, boolean showBreakingOverlay, RandomOffsetFunction randomOffset) {
    }
}
