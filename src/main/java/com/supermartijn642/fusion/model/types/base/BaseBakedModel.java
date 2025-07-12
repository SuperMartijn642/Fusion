package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BlockStateModel {

    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<BlockState> STATE_PROPERTY = new ModelProperty<>();

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final Map<Optional<ChunkSectionLayer>,List<TaggedBakedQuad>[]> blockMesh;
    private final Collection<ChunkSectionLayer> blockRenderTypes;
    private final boolean shouldCheckOriginalBlockRenderTypes;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final TextureAtlasSprite particleIcon;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, TextureAtlasSprite particleIcon, ChunkSectionLayer forgeRenderType){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleIcon = particleIcon;

        // Create the block mesh
        Map<Optional<ChunkSectionLayer>,List<TaggedBakedQuad>[]> blockMesh = new HashMap<>();
        Set<Optional<ChunkSectionLayer>> blockRenderTypes = new HashSet<>();
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
            Optional<ChunkSectionLayer> layer = FusionClient.getChunkLayer(quad.renderType());
            if(layer.isEmpty() && forgeRenderType != null)
                layer = Optional.of(forgeRenderType);
            blockRenderTypes.add(layer);
            int cullIndex = cullIndex(quad.cullDirection());
            //noinspection unchecked
            List<TaggedBakedQuad>[] mesh = blockMesh.computeIfAbsent(layer, r -> new List[7]);
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(finishedQuad);
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = blockRenderTypes.stream().filter(Optional::isPresent).map(Optional::get).toList();
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(Optional.<ChunkSectionLayer>empty());
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = this.blockMesh.values().stream().map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).toList();
        }
    }

    @Override
    public List<BlockModelPart> collectParts(RandomSource random, ModelData data, @Nullable ChunkSectionLayer renderType){
        return List.of(new BlockModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                return BaseBakedModel.this.getQuads(data.get(STATE_PROPERTY), cullDirection, random, data, renderType);
            }

            @Override
            public boolean useAmbientOcclusion(){
                return BaseBakedModel.this.hasAmbientOcclusion;
            }

            @Override
            public TextureAtlasSprite particleIcon(){
                return BaseBakedModel.this.particleIcon;
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData data, @Nullable ChunkSectionLayer renderType){
        parts.addAll(this.collectParts(random, data, renderType));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        parts.addAll(this.collectParts(random, ModelData.EMPTY, null));
    }

    private List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData data, @Nullable ChunkSectionLayer renderType){
        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh.get(Optional.of(renderType));
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            //noinspection deprecation
            if(this.shouldCheckOriginalBlockRenderTypes && state != null && ItemBlockRenderTypes.getRenderLayers(state).contains(renderType)){
                mesh = this.blockMesh.get(Optional.<ChunkSectionLayer>empty());
                List<TaggedBakedQuad> additionalQuads = mesh == null ? null : mesh[cullIndex(cullDirection)];
                if(additionalQuads != null){
                    if(quads == null)
                        quads = additionalQuads;
                    else{
                        List<TaggedBakedQuad> combined = new ArrayList<>(quads.size() + additionalQuads.size());
                        combined.addAll(quads);
                        combined.addAll(additionalQuads);
                        quads = combined;
                    }
                }
            }
            if(quads == null)
                quads = Collections.emptyList();
        }

        // If there's no special quads, just return the quads as is
        if(!this.hasSpecialQuads){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }

        // Get the position from the model data
        BlockPos pos = data.get(POSITION_PROPERTY);
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
    public Collection<ChunkSectionLayer> getRenderTypes(BlockState state, RandomSource rand, ModelData data){
        if(this.shouldCheckOriginalBlockRenderTypes){
            // There's no way to know the render types beforehand through NeoForge's API, so just merge them here with the fixed render types
            //noinspection deprecation
            Collection<ChunkSectionLayer> layers = EnumSet.copyOf(ItemBlockRenderTypes.getRenderLayers(state));
            layers.addAll(this.blockRenderTypes);
            return layers;
        }
        return this.blockRenderTypes;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        if(!this.hasSpecialQuads && !this.shouldCheckOriginalBlockRenderTypes)
            return ModelData.EMPTY;
        return ModelData.builder().with(POSITION_PROPERTY, pos).with(STATE_PROPERTY, state).build();
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
