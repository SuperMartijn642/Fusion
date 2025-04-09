package com.supermartijn642.fusion.model.modifiers.block;

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

import java.util.List;
import java.util.function.Predicate;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierBakedModel implements BlockStateModel {

    private final BlockStateModel original;
    private final List<BlockStateModel> models;

    public BlockModelModifierBakedModel(BlockStateModel original, List<BlockStateModel> models){
        this.original = original;
        this.models = List.copyOf(models);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        this.original.emitQuads(emitter, blockView, pos, state, random, cullTest);
        for(BlockStateModel model : this.models)
            model.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> list){
        this.original.collectParts(random, list);
    }

    @Override
    public List<BlockModelPart> collectParts(RandomSource random){
        return this.original.collectParts(random);
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
