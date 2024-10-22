package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.model.types.connecting.SurroundingBlockCache;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel.BLOCK_CACHE;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final List<TaggedBakedQuad>[][] blockMesh; // indexed by render layer ordinal, cull direction
    private final List<BakedQuad> itemMesh;
    private final List<BlockRenderLayer> blockRenderTypes;
    private final boolean shouldCheckOriginalBlockRenderTypes;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final TextureAtlasSprite particleIcon;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, TextureAtlasSprite particleIcon, ItemCameraTransforms transforms, ItemOverrideList overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        // noinspection unchecked
        List<TaggedBakedQuad>[][] blockMesh = new List[BlockRenderLayer.values().length + 1][];
        Set<BlockRenderLayer> blockRenderTypes = new HashSet<>();
        List<BakedQuad> itemMesh = new ArrayList<>();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.emissive(quad.emissive());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), mutableQuad.lightmap(i) >> 20 & 0xffff);
                    int block = Math.max(quad.lightEmission(), (mutableQuad.lightmap(i) & 0xffff) >> 4);
                    mutableQuad.lightmap(i, (sky << 20 | block << 4));
                }
            }
            // Tag quads which need additional processing
            int spriteIndex = -1;
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }

            TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(), quad.textureType(), spriteIndex);
            // Add the block quads
            BlockRenderLayer renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            List<TaggedBakedQuad>[] mesh = blockMesh[renderType == null ? 0 : renderType.ordinal() + 1];
            if(mesh == null){
                // noinspection unchecked
                mesh = new List[7];
                blockMesh[renderType == null ? 0 : renderType.ordinal() + 1] = mesh;
            }
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(finishedQuad);
            // Add the item quads
            itemMesh.add(mutableQuad.toBakedQuad());
        }
        this.blockMesh = blockMesh;
        this.blockRenderTypes = blockRenderTypes.stream().filter(Objects::nonNull).collect(Collectors.toList());
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(null);
        this.itemMesh = ImmutableList.copyOf(itemMesh);
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).collect(Collectors.toList());
        this.hasSpecialQuads = hasSpecialQuads;

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = Arrays.stream(this.blockMesh).filter(Objects::nonNull).map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed, @Nullable BlockRenderLayer renderType){
        // If the block state is null, assume this call is intended for item rendering
        if(state == null)
            return cullDirection == null ? this.itemMesh : Collections.emptyList();

        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh[renderType.ordinal() + 1];
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(this.shouldCheckOriginalBlockRenderTypes && state.getBlock().getBlockLayer() == renderType){
                mesh = this.blockMesh[0];
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

        // Get the block cache from the model data
        SurroundingBlockCache blockCache = BLOCK_CACHE.get();
        // If the position is absent, just return the quads
        if(blockCache == null){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.bakedQuad);
            return bakedQuads;
        }
        // Get the position from the model data
        BlockPos pos = blockCache.getRealPos();

        // Push a transform which maps any connecting texture quads to the correct uv
        ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
        Random random = null;
        MutableQuad mutableQuad = new MutableQuad();
        for(TaggedBakedQuad quad : quads){
            // Process special texture type quads
            if(quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS){
                // Get the sprite
                TextureAtlasSprite sprite = this.sprites.get(quad.spriteIndex);

                mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                if(quad.textureType == DefaultTextureTypes.RANDOM){
                    // Handle random texture type
                    if(random == null) random = new Random();
                    RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getFace(), random, (RandomTextureSprite)sprite);
                }else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getFace(), (ContinuousTextureSprite)sprite);
                bakedQuads.add(mutableQuad.toBakedQuad());
            }else
                bakedQuads.add(quad.bakedQuad);
        }
        return bakedQuads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        return this.getQuads(state, cullDirection, seed, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public List<BlockRenderLayer> getBlockRenderTypes(){
        return this.blockRenderTypes;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.particleIcon;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.overrides;
    }

    private static int cullIndex(EnumFacing cullDirection){
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
