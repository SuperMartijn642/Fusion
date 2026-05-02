package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BlockStateModel {

    private final List<TaggedBakedQuad>[] blockMesh;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final Material.Baked particleMaterial;
    private final @BakedQuad.MaterialFlags int materialFlags;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, Material.Baked particleMaterial, ModelBaker.Interner interner){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleMaterial = particleMaterial;

        // Create block and item meshes from the quads
        //noinspection unchecked
        List<TaggedBakedQuad>[] blockMesh = new List[7];
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        @BakedQuad.MaterialFlags int materialFlags = 0;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            quad.fill(mutableQuad);
            mutableQuad.ambientOcclusion(!quad.emissive() && hasAmbientOcclusion);
            // Tag quads which need additional processing
            int spriteIndex = -1;
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().materialInfo().sprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }

            TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(interner), quad.textureType(), spriteIndex);
            // Add the block quads
            int cullIndex = cullIndex(quad.cullDirection());
            if(blockMesh[cullIndex] == null)
                blockMesh[cullIndex] = new ArrayList<>();
            blockMesh[cullIndex].add(finishedQuad);
            materialFlags |= finishedQuad.bakedQuad.materialInfo().flags();
        }
        this.blockMesh = blockMesh;
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;
        this.materialFlags = materialFlags;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts){
        parts.add(new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                return BaseBakedModel.this.getQuads(level, pos, state, cullDirection, random);
            }

            @Override
            public boolean useAmbientOcclusion(){
                return BaseBakedModel.this.hasAmbientOcclusion;
            }

            @Override
            public Material.Baked particleMaterial(){
                return BaseBakedModel.this.particleMaterial;
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return BaseBakedModel.this.materialFlags;
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        this.collectParts(null, null, null, random, parts);
    }

    private List<BakedQuad> getQuads(@Nullable BlockAndTintGetter blockView, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        List<TaggedBakedQuad> quads = this.blockMesh[cullIndex(cullDirection)];
        if(quads == null)
            return Collections.emptyList();

        // If there's no special quads, just return the quads as is
        if(!this.hasSpecialQuads){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }

        // If the position is absent, just return the quads
        if(pos == null){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }

        // Push a transform which maps any connecting texture quads to the correct uv
        ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
        MutableQuad mutableQuad = new MutableQuad();
        for(TaggedBakedQuad quad : quads){
            // Process special texture type quads
            if(quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS){
                // Get the sprite
                TextureAtlasSprite sprite = this.sprites.get(quad.spriteIndex);

                mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                if(quad.textureType == DefaultTextureTypes.RANDOM)
                    // Handle random texture type
                    RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.direction(), random, (RandomTextureSprite)sprite);
                else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.direction(), (ContinuousTextureSprite)sprite);
                bakedQuads.add(mutableQuad.toBakedQuad(null));
            }else
                bakedQuads.add(quad.bakedQuad);
        }
        return bakedQuads;
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return this.hasSpecialQuads ? Pair.of(this, pos) : this;
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    private static int cullIndex(Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    private static class TaggedBakedQuad {
        final BakedQuad bakedQuad;
        final TextureType<?> textureType;
        final int spriteIndex;

        private TaggedBakedQuad(BakedQuad bakedQuad, TextureType<?> textureType, int spriteIndex){
            this.bakedQuad = bakedQuad;
            this.textureType = textureType;
            this.spriteIndex = spriteIndex;
        }
    }
}
