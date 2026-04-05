package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.FusionClient;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
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
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        long seed = random.nextLong();
        if(!this.showBreakingOverlay && FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null){
            random.setSeed(seed);
            this.original.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }
        for(BlockStateModel model : this.models){
            random.setSeed(seed);
            model.emitQuads(emitter, blockView, pos, state, random, cullTest);
        }
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random){
        List<Object> keys = new ArrayList<>(this.models.size() + 2);
        keys.add(this);
        long seed = random.nextLong();
        for(BlockStateModel model : this.models){
            random.setSeed(seed);
            Object subKey = model.createGeometryKey(blockView, pos, state, random);
            if(subKey == null)
                return null;
            keys.add(subKey);
        }
        return keys;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        long seed = random.nextLong();
        for(BlockStateModel model : this.models){
            random.setSeed(seed);
            model.collectParts(random, parts);
        }
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.original.particleIcon();
    }

    @Override
    public TextureAtlasSprite particleSprite(BlockAndTintGetter blockView, BlockPos pos, BlockState state){
        return this.original.particleSprite(blockView, pos, state);
    }
}
