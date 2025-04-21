package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BlockStateModel {

    /*
     * Quads are tagged if they need further processing.
     * The tag consists of:
     *  - 4 bits to indicate texture type
     *  - 8 bits to indicate sprite index
     */

    private final Mesh mesh;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final TextureAtlasSprite particleIcon;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, TextureAtlasSprite particleIcon){
        this.particleIcon = particleIcon;

        // Create the block mesh
        MutableMesh builder = Renderer.get().mutableMesh();
        QuadEmitter emitter = builder.emitter();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        for(BaseModelQuad quad : quads){
            RenderMaterial material = FusionClient.getRenderTypeMaterial(hasAmbientOcclusion, quad.renderType(), quad.emissive());
            emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
            // Tag quads which need additional processing
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                int type = quad.textureType() == DefaultTextureTypes.RANDOM ? 2 : 3;
                // Give each sprite a unique index
                int spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().sprite(), o -> sprites.size());
                // Pack the type and sprite index into the tag
                emitter.tag(type | (spriteIndex << 4));
                hasSpecialQuads = true;
            }
            emitter.emit();
        }
        this.mesh = builder.immutableCopy();
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;
    }

    @Override
    public List<BlockModelPart> collectParts(RandomSource randomSource){
        return List.of();
    }

    @Override
    public void collectParts(RandomSource randomSource, List<BlockModelPart> list){
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        if(!this.hasSpecialQuads){
            this.mesh.outputTo(emitter);
            return;
        }

        // If a quad is going to get culled anyway, don't bother processing it
        boolean[] culledFaces = {
            cullTest.test(Direction.DOWN),
            cullTest.test(Direction.UP),
            cullTest.test(Direction.NORTH),
            cullTest.test(Direction.SOUTH),
            cullTest.test(Direction.WEST),
            cullTest.test(Direction.EAST)
        };

        // Process special texture type quads
        MutableQuad mutableQuad = new MutableQuad();
        emitter.pushTransform(
            quad -> {
                if(quad.tag() != 0){
                    // Ignore the quad if it will be culled anyway
                    Direction cullFace = quad.cullFace();
                    if(cullFace != null && culledFaces[cullFace.ordinal()])
                        return false;

                    // Unpack type and sprite index from the tag
                    int tag = quad.tag();
                    int type = quad.tag() & ((1 << 4) - 1);
                    int spriteIndex = (tag >> 4) & ((1 << 8) - 1);

                    // Get the sprite
                    TextureAtlasSprite sprite = this.sprites.get(spriteIndex);

                    // TODO fix this workaround
                    quad.tag((int)Math.floor((sprite.u1 + sprite.u0) / 2 * 65535) | (int)Math.floor((sprite.v1 + sprite.v0) / 2 * 65535) << 16);

                    // Handle random texture type
                    if(type == 2){
                        mutableQuad.set(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.nominalFace(), random, (RandomTextureSprite)sprite);
                        return true;
                    }
                    // Handle continuous texture type
                    if(type == 3){
                        mutableQuad.set(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.nominalFace(), (ContinuousTextureSprite)sprite);
                        return true;
                    }
                }
                return true;
            }
        );
        this.mesh.outputTo(emitter);
        emitter.popTransform();
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this.hasSpecialQuads ? pos : 0;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleIcon;
    }
}
