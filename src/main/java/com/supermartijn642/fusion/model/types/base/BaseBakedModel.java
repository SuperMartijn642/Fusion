package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.model.OriginalRenderTypeHelper;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final List<TaggedBakedQuad>[][] blockMesh; // indexed by render layer ordinal, cull direction
    private final List<BakedQuad> itemMesh;
    private final List<BlockRenderLayer> blockRenderTypes;
    private final boolean shouldCheckOriginalBlockRenderTypes;
    private final List<SpriteInstance> sprites;
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
        Set<BlockRenderLayer> blockRenderTypes = new LinkedHashSet<>();
        List<BakedQuad> itemMesh = new ArrayList<>();
        HashMap<SpriteInstance,Integer> sprites = new HashMap<>();
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
                spriteIndex = sprites.computeIfAbsent(quad.spriteInstance(), o -> sprites.size());
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

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData data, @Nullable BlockRenderLayer renderType){
        // If the block state is null, assume this call is intended for item rendering
        if(state == null)
            return cullDirection == null ? this.itemMesh : Collections.emptyList();

        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh[renderType.ordinal() + 1];
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(this.shouldCheckOriginalBlockRenderTypes && state != null && OriginalRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType)){
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

        // Get the position from the model data
        BlockPos pos = data.getData(POSITION_PROPERTY);
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
                SpriteInstance sprite = this.sprites.get(quad.spriteIndex);

                mutableQuad.fillFromBakedQuad(quad.bakedQuad);
                if(quad.textureType == DefaultTextureTypes.RANDOM)
                    // Handle random texture type
                    RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getDirection(), random, sprite);
                else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getDirection(), sprite);
                bakedQuads.add(mutableQuad.toBakedQuad());
            }else
                bakedQuads.add(quad.bakedQuad);
        }
        return bakedQuads;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @Nonnull Random random, @Nonnull IModelData data){
        return this.getQuads(state, cullDirection, random, data, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(state, cullDirection, random, EmptyModelData.INSTANCE, MinecraftForgeClient.getRenderLayer());
    }

    @Override
    public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer){
        if(this.blockRenderTypes.contains(layer))
            return true;
        return this.shouldCheckOriginalBlockRenderTypes && OriginalRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
    }

    @Override
    public IModelData getModelData(IEnviromentBlockReader level, BlockPos pos, BlockState state, IModelData data){
        if(!this.hasSpecialQuads)
            return EmptyModelData.INSTANCE;
        return new ModelDataMap.Builder().withInitial(POSITION_PROPERTY, pos).build();
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleIcon;
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.overrides;
    }

    private static int cullIndex(Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    private static class TaggedBakedQuad {
        final BakedQuad bakedQuad;
        final TextureType<?,?> textureType;
        final int spriteIndex;

        private TaggedBakedQuad(BakedQuad bakedQuad, TextureType<?,?> textureType, int spriteIndex){
            this.bakedQuad = bakedQuad;
            this.textureType = textureType;
            this.spriteIndex = spriteIndex;
        }
    }
}
