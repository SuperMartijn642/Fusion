package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BakedModel {

    private final BakedModel original;
    private final List<ConditionalModel> defaultModelOverrides;
    private final List<List<ConditionalModel>> appendModels;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;

    BlockModelModifierBakedModel(BakedModel original, List<ConditionalModel> defaultModelOverrides, List<List<ConditionalModel>> appendModels){
        this.original = original;
        this.defaultModelOverrides = defaultModelOverrides;
        this.appendModels = appendModels;

        // Resolve default context properties
        TextureAtlasSprite particleSprite = this.original.getParticleIcon();
        boolean ambientOcclusion = true;
        for(ConditionalModel override : this.defaultModelOverrides){
            if(override.conditions == null || override.conditions.test(null, null, null)){
                particleSprite = override.model.getParticleIcon();
                ambientOcclusion = override.model.useAmbientOcclusion();
                break;
            }
        }
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
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
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest){
        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        override.model.emitBlockQuads(emitter, level, state, pos, randomSupplier, cullTest);
                    break overrides;
                }
            }
            this.original.emitBlockQuads(emitter, level, state, pos, randomSupplier, cullTest);
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(level, pos, state)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay){
                        conditional.model.emitBlockQuads(emitter, level, state, pos, randomSupplier, cullTest);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        long seed = random.nextLong();
        random.setSeed(seed);

        // Check whether the breaking overlay is being rendered
        boolean isBreakingOverlay = FusionClient.isRenderingBreakingOverlay();

        // Collect all quads
        List<BakedQuad> quads = new ArrayList<>();

        // Default model
        overrides:
        {
            for(ConditionalModel override : this.defaultModelOverrides){
                if(override.conditions == null || override.conditions.test(null, null, null)){
                    if(!isBreakingOverlay || override.showBreakingOverlay)
                        quads.addAll(override.model.getQuads(state, cullDirection, random));
                    break overrides;
                }
            }
            quads.addAll(this.original.getQuads(state, cullDirection, random));
        }

        // Append models
        for(List<ConditionalModel> appendEntry : this.appendModels){
            // First model whose conditions are met is submitted
            for(ConditionalModel conditional : appendEntry){
                if(conditional.conditions == null || conditional.conditions.test(null, null, null)){
                    if(!isBreakingOverlay || conditional.showBreakingOverlay){
                        random.setSeed(seed);
                        quads.addAll(conditional.model.getQuads(state, cullDirection, random));
                    }
                    break;
                }
            }
        }
        return quads;
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.original.usesBlockLight();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.original.getTransforms();
    }

    record ConditionalModel(BakedModel model, @Nullable BlockStateModelPredicate conditions, boolean showBreakingOverlay, RandomOffsetFunction randomOffset) {
    }
}
