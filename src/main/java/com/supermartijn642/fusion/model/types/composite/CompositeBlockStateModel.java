package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
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
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBlockStateModel implements BlockStateModel {

    private final BlockStateModel defaultModel;
    private final List<ConditionalList> entries;
    private final int materialFlags;

    public CompositeBlockStateModel(BlockStateModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;

        // Resolve default values
        int materialFlags = 0;
        for(ConditionalList list : entries){
            for(ModelEntry entry : list.entries){
                materialFlags |= entry.model.materialFlags();
            }
        }
        this.materialFlags = materialFlags;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model != null){
                random.setSeed(seed);
                model.emitQuads(emitter, level, pos, state, random, cullTest);
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(null, null, null);
            if(model != null){
                random.setSeed(seed);
                model.collectParts(random, parts);
            }
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<Object> keys = new ArrayList<>();
        keys.add(this);
        long seed = random.nextLong();
        for(ConditionalList list : this.entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate != null){
                    if(!entry.predicate.testForBlockState(level, pos, state)){
                        keys.add(false);
                        continue;
                    }
                    keys.add(true);
                }
                random.setSeed(seed);
                Object key = entry.model.createGeometryKey(level, pos, state, random);
                if(key == null)
                    return null;
                keys.add(key);
            }
        }
        return keys;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state){
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model != null)
                return model.particleMaterial(level, pos, state);
        }
        return this.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.defaultModel.particleMaterial();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        long seed = random.nextLong();
        int materialFlags = 0;
        for(ConditionalList list : this.entries){
            BlockStateModel model = list.get(level, pos, state);
            if(model != null){
                random.setSeed(seed);
                materialFlags |= model.materialFlags(level, pos, state, random);
            }
        }
        return materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, int flag){
        return (this.materialFlags(level, pos, state, random) & flag) != 0;
    }

    @Override
    public int materialFlags(){
        return this.materialFlags;
    }

    @Override
    public boolean hasMaterialFlag(int flag){
        return (this.materialFlags & flag) != 0;
    }

    public record ConditionalList(List<ModelEntry> entries) {
        @Nullable
        BlockStateModel get(BlockAndTintGetter level, BlockPos pos, BlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }
    }

    public record ModelEntry(BlockStateModel model, ModelPredicate predicate) {
    }
}
