package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BlockStateModel {

    private final Map<RenderType,List<TaggedBakedQuad>[]> blockMesh;
    private final List<RenderType> blockRenderTypes;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final TextureAtlasSprite particleIcon;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, TextureAtlasSprite particleIcon, RenderType neoforgeRenderType){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleIcon = particleIcon;

        // Create block and item meshes from the quads
        Map<RenderType,List<TaggedBakedQuad>[]> blockMesh = new HashMap<>();
        Set<RenderType> blockRenderTypes = new HashSet<>();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.ambientOcclusion(!quad.emissive() && hasAmbientOcclusion);
            mutableQuad.emissive(quad.emissive());
            // Tag quads which need additional processing
            int spriteIndex = -1;
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().sprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }

            TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(), quad.textureType(), spriteIndex);
            // Add the block quads
            RenderType renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            if(renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER && neoforgeRenderType != null)
                renderType = neoforgeRenderType;
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            //noinspection unchecked
            List<TaggedBakedQuad>[] mesh = blockMesh.computeIfAbsent(renderType, r -> new List[7]);
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(finishedQuad);
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = List.copyOf(blockRenderTypes);
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts){
        for(RenderType renderType : this.blockRenderTypes){
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    return BaseBakedModel.this.getQuads(level, pos, state, cullDirection, random, renderType);
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return BaseBakedModel.this.hasAmbientOcclusion;
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return BaseBakedModel.this.particleIcon;
                }

                @Override
                public RenderType getRenderType(BlockState state){
                    //noinspection deprecation
                    return renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? ItemBlockRenderTypes.getChunkRenderType(state) : renderType;
                }
            });
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(null, null, null, random, parts);
    }

    private List<BakedQuad> getQuads(@Nullable BlockAndTintGetter blockView, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, @NotNull RenderType renderType){
        List<TaggedBakedQuad> quads = this.blockMesh.get(renderType)[cullIndex(cullDirection)];
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
                bakedQuads.add(mutableQuad.toBakedQuad());
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
    public TextureAtlasSprite particleIcon(){
        return this.particleIcon;
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
