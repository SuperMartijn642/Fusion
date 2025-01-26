package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.model.ItemBakedModel;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedOverrides;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel {

    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final List<BakedQuad> completeItemMesh;
    private final Map<RenderType,List<TaggedBakedQuad>[]> blockMesh;
    private final Map<RenderType,List<BakedQuad>> itemMesh;
    private final ChunkRenderTypeSet blockRenderTypes;
    private final List<RenderType> itemRenderTypes;
    private final boolean shouldCheckOriginalItemRenderTypes, shouldCheckOriginalBlockRenderTypes;
    private final ItemBakedModel itemModel;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private final BakedOverrides overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms, BakedOverrides overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        Map<RenderType,List<TaggedBakedQuad>[]> blockMesh = new HashMap<>();
        Set<RenderType> blockRenderTypes = new HashSet<>();
        Map<RenderType,List<BakedQuad>> itemMesh = new HashMap<>();
        Set<RenderType> itemRenderTypes = new HashSet<>();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.ambientOcclusion(hasAmbientOcclusion);
            mutableQuad.emissive(quad.emissive());
            // Tag quads which need additional processing
            int spriteIndex = -1;
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
                hasSpecialQuads = true;
            }

            TaggedBakedQuad finishedQuad = new TaggedBakedQuad(mutableQuad.toBakedQuad(), quad.textureType(), spriteIndex);
            // Add the block quads
            RenderType renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            //noinspection unchecked
            List<TaggedBakedQuad>[] mesh = blockMesh.computeIfAbsent(renderType, r -> new List[7]);
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(finishedQuad);
            // Add the item quads
            RenderType itemRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                : RenderTypeHelper.getEntityRenderType(renderType);
            itemRenderTypes.add(itemRenderType);
            List<BakedQuad> itemQuads = itemMesh.computeIfAbsent(itemRenderType, k -> new ArrayList<>());
            itemQuads.add(mutableQuad.toBakedQuad());
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = ChunkRenderTypeSet.of(blockRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList());
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.itemMesh = Map.copyOf(itemMesh);
        this.itemRenderTypes = itemRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.shouldCheckOriginalItemRenderTypes = itemRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = this.blockMesh.values().stream().map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).toList();
        }
        this.completeItemMesh = itemRenderTypes.stream().map(this.itemMesh::get).flatMap(List::stream).toList();

        // Create a model to return the item quads
        this.itemModel = new ItemBakedModel(this) {
            @Override
            protected List<BakedQuad> getQuads(ItemStack stack, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
                if(renderType == null)
                    return BaseBakedModel.this.completeItemMesh;

                List<BakedQuad> quads = BaseBakedModel.this.itemMesh.get(renderType);
                //noinspection deprecation
                if(BaseBakedModel.this.shouldCheckOriginalItemRenderTypes && ItemBlockRenderTypes.getRenderType(stack) == renderType){
                    List<BakedQuad> additionalQuads = BaseBakedModel.this.itemMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
                    if(additionalQuads != null){
                        if(quads == null)
                            quads = additionalQuads;
                        else{
                            List<BakedQuad> combined = new ArrayList<>(quads.size() + additionalQuads.size());
                            combined.addAll(quads);
                            combined.addAll(additionalQuads);
                            quads = combined;
                        }
                    }
                }
                return quads == null ? Collections.emptyList() : quads;
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData data, @Nullable RenderType renderType){
        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh.get(renderType);
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            //noinspection deprecation
            if(this.shouldCheckOriginalBlockRenderTypes && state != null && ItemBlockRenderTypes.getRenderLayers(state).contains(renderType)){
                mesh = this.blockMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
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
                    RandomTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getDirection(), random, (RandomTextureSprite)sprite);
                else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.bakedQuad.getDirection(), (ContinuousTextureSprite)sprite);
                bakedQuads.add(mutableQuad.toBakedQuad());
            }else
                bakedQuads.add(quad.bakedQuad);
        }
        return bakedQuads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random){
        return this.getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data){
        if(this.shouldCheckOriginalBlockRenderTypes){
            // There's no way to know the render types beforehand through NeoForge's API, so just merge them here with the fixed render types
            //noinspection deprecation
            return ChunkRenderTypeSet.union(
                ItemBlockRenderTypes.getRenderLayers(state),
                this.blockRenderTypes
            );
        }
        return this.blockRenderTypes;
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack){
        if(this.shouldCheckOriginalItemRenderTypes){
            // There's no way to know the render types beforehand through NeoForge's API, so just merge them here with the fixed render types
            RenderType renderType = RenderTypeHelper.getFallbackItemRenderType(stack, this);
            if(!this.itemRenderTypes.contains(renderType)){
                ArrayList<RenderType> combined = new ArrayList<>(this.itemRenderTypes.size() + 1);
                combined.addAll(this.itemRenderTypes);
                combined.add(renderType);
                return combined;
            }
        }
        return this.itemRenderTypes;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack){
        this.itemModel.set(stack);
        return this.itemModel.asList();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        if(!this.hasSpecialQuads)
            return ModelData.EMPTY;
        return ModelData.builder().with(POSITION_PROPERTY, pos).build();
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
    public boolean usesBlockLight(){
        return this.usesBlockLight;
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
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public BakedOverrides overrides(){
        return this.overrides;
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
