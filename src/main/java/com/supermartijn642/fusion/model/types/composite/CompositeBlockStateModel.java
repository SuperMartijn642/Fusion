package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
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
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBlockStateModel implements BakedModel {

    private final BakedModel defaultModel;
    private final List<ConditionalList> entries;

    public CompositeBlockStateModel(BakedModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;
    }

    @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest){
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(level, pos, state);
            if(model != null)
                model.emitBlockQuads(emitter, level, state, pos, randomSupplier, cullTest);
        }
    }

    @Override
    public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier){
        for(ConditionalList list : this.entries){
            if(list.itemModel != null)
                list.itemModel.emitItemQuads(emitter, randomSupplier);
        }
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        long seed = random.nextLong();
        List<BakedQuad> quads = new ArrayList<>();
        for(ConditionalList list : this.entries){
            BakedModel model = list.get(null, null, state);
            if(model != null){
                random.setSeed(seed);
                quads.addAll(model.getQuads(state, cullDirection, random));
            }
        }
        return quads;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.defaultModel.getParticleIcon();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.defaultModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.defaultModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight(){
        return this.defaultModel.usesBlockLight();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.defaultModel.getTransforms();
    }

    public record ConditionalList(List<ModelEntry> entries, @Nullable BakedModel itemModel) {
        @Nullable
        BakedModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BakedModel model, ModelPredicate predicate) {
    }
}
