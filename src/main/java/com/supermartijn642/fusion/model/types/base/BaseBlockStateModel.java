package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
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

import java.util.List;
import java.util.function.Predicate;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private static final Direction[] CULL_DIRECTIONS = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final List<Part> parts;
    private final ModelMaterial.Resolved particleMaterial;
    private final int materialFlags;

    public BaseBlockStateModel(List<Part> parts, ModelMaterial.Resolved particleMaterial, int materialFlags){
        this.parts = parts;
        this.particleMaterial = particleMaterial;
        this.materialFlags = materialFlags;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        for(Part part : this.parts){
            for(Direction cullDirection : CULL_DIRECTIONS){
                // Skip direction if it doesn't pass the cull test
                if(cullTest.test(cullDirection))
                    continue;

                emitter.cullFace(cullDirection);
                emitter.shadeMode(ShadeMode.VANILLA);

                MutableQuad mutableQuad = null;
                for(QuadAccess quad : part.quads().get(cullDirection)){
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(sprite == null){
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                        continue;
                    }

                    // Process special texture type quads
                    TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                    if(textureType == DefaultTextureTypes.RANDOM){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), random, sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else{
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                    }
                }
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output){
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return null;
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial.toBakedMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    public record Part(CullableQuads quads, ModelMaterial.Resolved particleMaterial, int materialFlags) {
    }
}
