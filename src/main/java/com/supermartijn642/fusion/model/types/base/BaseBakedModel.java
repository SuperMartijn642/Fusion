package com.supermartijn642.fusion.model.types.base;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.ItemBakedModel;
import com.supermartijn642.fusion.model.MutableQuad;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel {

    private final List<BakedQuad>[] completeBlockMesh;
    private final List<BakedQuad> completeItemMesh;
    private final Map<RenderType,List<BakedQuad>[]> blockMesh;
    private final Map<RenderType,List<BakedQuad>> itemMesh;
    private final List<RenderType> blockRenderTypes;
    private final List<RenderType> itemRenderTypes, itemRenderTypesFabulous;
    private final boolean shouldCheckOriginalItemRenderTypes, shouldCheckOriginalBlockRenderTypes;
    private final ItemBakedModel itemModel;
    private final List<Pair<BakedModel,RenderType>> itemPasses, itemPassesFabulous;
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
        Map<RenderType,List<BakedQuad>[]> blockMesh = new HashMap<>();
        Set<RenderType> blockRenderTypes = new HashSet<>();
        Map<RenderType,List<BakedQuad>> itemMesh = new HashMap<>();
        Set<RenderType> itemRenderTypes = new HashSet<>(), itemRenderTypesFabulous = new HashSet<>();
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
            BakedQuad finishedQuad = mutableQuad.toBakedQuad();
            // Add the block quads
            RenderType renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
            blockRenderTypes.add(renderType);
            int cullIndex = cullIndex(quad.cullDirection());
            //noinspection unchecked
            List<BakedQuad>[] mesh = blockMesh.computeIfAbsent(renderType, r -> new List[7]);
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(mutableQuad.toBakedQuad());
            // Add the item quads
            RenderType itemRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                : renderType == RenderType.translucent() ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet();
            itemRenderTypes.add(itemRenderType);
            List<BakedQuad> itemQuads = itemMesh.get(renderType);
            if(itemQuads == null){
                itemQuads = new ArrayList<>();
                itemMesh.put(renderType, itemQuads);
                RenderType fabulousRenderType = renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER ? FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER
                    : renderType == RenderType.translucent() ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet();
                itemRenderTypesFabulous.add(fabulousRenderType);
                itemMesh.put(fabulousRenderType, itemQuads);
            }
            itemQuads.add(finishedQuad);
        }
        this.blockMesh = Map.copyOf(blockMesh);
        this.blockRenderTypes = blockRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.shouldCheckOriginalBlockRenderTypes = blockRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
        this.itemMesh = Map.copyOf(itemMesh);
        this.itemRenderTypes = itemRenderTypes.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.itemRenderTypesFabulous = itemRenderTypesFabulous.stream().filter(r -> r != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER).toList();
        this.shouldCheckOriginalItemRenderTypes = itemRenderTypes.contains(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);

        //noinspection unchecked
        this.completeBlockMesh = new List[7];
        for(int i = 0; i < 7; i++){
            int cullIndex = i;
            this.completeBlockMesh[i] = this.blockMesh.values().stream().map(arr -> arr[cullIndex]).filter(Objects::nonNull).flatMap(List::stream).toList();
        }
        this.completeItemMesh = this.itemRenderTypes.stream().map(this.itemMesh::get).flatMap(List::stream).toList();

        // Create a model to return the item quads
        this.itemModel = new ItemBakedModel(this) {
            @Override
            protected List<BakedQuad> getQuads(ItemStack stack, boolean fabulous, @Nonnull Random random, @Nonnull IModelData data, @Nullable RenderType renderType){
                if(renderType == null)
                    return BaseBakedModel.this.completeItemMesh;

                List<BakedQuad> quads = BaseBakedModel.this.itemMesh.get(renderType);
                if(BaseBakedModel.this.shouldCheckOriginalItemRenderTypes && ItemBlockRenderTypes.getRenderType(stack, fabulous) == renderType){
                    List<BakedQuad> additionalQuads = BaseBakedModel.this.itemMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
                    if(additionalQuads != null){
                        if(quads == null)
                            quads = additionalQuads;
                        List<BakedQuad> combined = new ArrayList<>(quads.size() + additionalQuads.size());
                        combined.addAll(quads);
                        combined.addAll(additionalQuads);
                        quads = combined;
                    }
                }
                return quads == null ? Collections.emptyList() : quads;
            }
        };
        this.itemPasses = this.itemRenderTypes.stream().map(r -> Pair.of((BakedModel)this.itemModel, r)).toList();
        this.itemPassesFabulous = this.itemRenderTypesFabulous.stream().map(r -> Pair.of((BakedModel)this.itemModel, r)).toList();
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random, IModelData data, @Nullable RenderType renderType){
        if(renderType == null)
            return this.completeBlockMesh[cullIndex(cullDirection)];

        List<BakedQuad>[] mesh = this.blockMesh.get(renderType);
        List<BakedQuad> quads = mesh == null ? null : mesh[cullIndex(cullDirection)];
        //noinspection deprecation
        if(this.shouldCheckOriginalBlockRenderTypes && state != null && ItemBlockRenderTypes.getChunkRenderType(state) == renderType){
            mesh = this.blockMesh.get(FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER);
            List<BakedQuad> additionalQuads = mesh == null ? null : mesh[cullIndex(cullDirection)];
            if(additionalQuads != null){
                if(quads == null)
                    quads = additionalQuads;
                List<BakedQuad> combined = new ArrayList<>(quads.size() + additionalQuads.size());
                combined.addAll(quads);
                combined.addAll(additionalQuads);
                quads = combined;
            }
        }
        return quads == null ? Collections.emptyList() : quads;
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

    public List<RenderType> getBlockRenderTypes(){
        return this.blockRenderTypes;
    }

    @Override
    public List<Pair<BakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
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
}
