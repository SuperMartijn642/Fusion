package com.supermartijn642.fusion.model.types.base;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.ItemBakedModel;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.model.OriginalRenderTypeHelper;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel, CustomRenderTypeBakedModel {

    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();

    private final List<TaggedBakedQuad>[] completeBlockMesh;
    private final List<BakedQuad> completeItemMesh;
    private final Map<RenderType,List<TaggedBakedQuad>[]> blockMesh;
    private final Map<RenderType,List<BakedQuad>> itemMesh;
    private final List<RenderType> blockRenderTypes;
    private final List<RenderType> itemRenderTypes, itemRenderTypesFabulous;
    private final boolean shouldCheckOriginalItemRenderTypes, shouldCheckOriginalBlockRenderTypes;
    private final ItemBakedModel itemModel;
    private final List<Pair<BakedModel,RenderType>> itemPasses, itemPassesFabulous;
    private final List<SpriteInstance> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms, ItemOverrides overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        Map<RenderType,List<TaggedBakedQuad>[]> blockMesh = new HashMap<>();
        Set<RenderType> blockRenderTypes = new LinkedHashSet<>();
        Map<RenderType,List<BakedQuad>> itemMesh = new HashMap<>();
        Set<RenderType> itemRenderTypes = new LinkedHashSet<>(), itemRenderTypesFabulous = new LinkedHashSet<>();
        HashMap<SpriteInstance,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            mutableQuad.fillFromBakedQuad(quad.bakedQuad());
            mutableQuad.emissive(quad.emissive());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), LightTexture.sky(mutableQuad.lightmap(i)));
                    int block = Math.max(quad.lightEmission(), LightTexture.block(mutableQuad.lightmap(i)));
                    mutableQuad.lightmap(i, LightTexture.pack(sky, block));
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
                : renderType == RenderType.translucent() ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet();
            itemRenderTypes.add(itemRenderType);
            List<BakedQuad> itemQuads = itemMesh.get(renderType);
            if(itemQuads == null){
                itemQuads = new ArrayList<>();
                itemMesh.put(itemRenderType, itemQuads);
                RenderType fabulousRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                    : renderType == RenderType.translucent() ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet();
                itemRenderTypesFabulous.add(fabulousRenderType);
                itemMesh.put(fabulousRenderType, itemQuads);
            }
            itemQuads.add(mutableQuad.toBakedQuad());
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = blockRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.itemMesh = Map.copyOf(itemMesh);
        this.itemRenderTypes = itemRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.itemRenderTypesFabulous = itemRenderTypesFabulous.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.shouldCheckOriginalItemRenderTypes = itemRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = blockRenderTypes.stream().map(r -> this.blockMesh.get(r)[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).toList();
        }
        this.completeItemMesh = itemRenderTypes.stream().map(this.itemMesh::get).flatMap(List::stream).toList();

        // Create a model to return the item quads
        this.itemModel = new ItemBakedModel(this) {
            @Override
            protected List<BakedQuad> getQuads(ItemStack stack, boolean fabulous, @NotNull Random random, @NotNull IModelData data, @Nullable RenderType renderType){
                if(renderType == null)
                    return BaseBakedModel.this.completeItemMesh;

                List<BakedQuad> quads = BaseBakedModel.this.itemMesh.get(renderType);
                if(BaseBakedModel.this.shouldCheckOriginalItemRenderTypes && ItemBlockRenderTypes.getRenderType(stack, fabulous) == renderType){
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
        this.itemPasses = this.itemRenderTypes.stream().map(r -> Pair.of((BakedModel)this.itemModel, r)).toList();
        this.itemPassesFabulous = this.itemRenderTypesFabulous.stream().map(r -> Pair.of((BakedModel)this.itemModel, r)).toList();
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData data, @Nullable RenderType renderType){
        List<TaggedBakedQuad> quads;
        if(renderType == null)
            quads = this.completeBlockMesh[cullIndex(cullDirection)];
        else{
            List<TaggedBakedQuad>[] mesh = this.blockMesh.get(renderType);
            quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(this.shouldCheckOriginalBlockRenderTypes && state != null && OriginalRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType)){
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

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random, @NotNull IModelData data){
        return this.getQuads(state, cullDirection, random, data, MinecraftForgeClient.getRenderType());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(state, cullDirection, random, EmptyModelData.INSTANCE, MinecraftForgeClient.getRenderType());
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        if(this.blockRenderTypes.contains(layer))
            return true;
        return this.shouldCheckOriginalBlockRenderTypes && OriginalRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        this.itemModel.set(stack, fabulous);
        if(this.shouldCheckOriginalItemRenderTypes){
            // There's no way to know the render types beforehand through Forge's API, so just merge them here with the fixed render types
            RenderType renderType = ItemBlockRenderTypes.getRenderType(stack, fabulous);
            if(!(fabulous ? this.itemRenderTypesFabulous : this.itemRenderTypes).contains(renderType)){
                ArrayList<Pair<BakedModel,RenderType>> combined = new ArrayList<>((fabulous ? this.itemPassesFabulous : this.itemPasses).size() + 1);
                combined.addAll(fabulous ? this.itemPassesFabulous : this.itemPasses);
                combined.add(Pair.of(this.itemModel, renderType));
                return combined;
            }
        }
        return fabulous ? this.itemPassesFabulous : this.itemPasses;
    }

    @Override
    public boolean isLayered(){
        return true;
    }

    @Override
    public IModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, IModelData data){
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
    public ItemOverrides getOverrides(){
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
