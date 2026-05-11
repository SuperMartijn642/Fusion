package com.supermartijn642.fusion.model.types.base;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemCameraTransforms transforms;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, boolean ambientOcclusion, BlockModel.GuiLight guiLight, boolean isGui3d, ItemCameraTransforms transforms){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
    }

    @Override
    public @NotNull IModelData getModelData(@NotNull IBlockDisplayReader level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull IModelData modelData){
        return new ModelDataMap.Builder()
            .withInitial(ConnectingBakedModel.POSITION_PROPERTY, pos)
            .build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random, @NotNull IModelData modelData){
        BlockPos pos = modelData.getData(ConnectingBakedModel.POSITION_PROPERTY);

        // Get whether the giving render type is the default render type
        RenderType renderType = MinecraftForgeClient.getRenderLayer();
        boolean isDefaultRenderType;
        if(renderType != null){
            //noinspection deprecation
            RenderType defaultRenderType = state == null ?
                RenderType.solid() :
                RenderTypeLookup.getChunkRenderType(state);
            isDefaultRenderType = renderType == defaultRenderType;
        }else
            isDefaultRenderType = true;

        // Collect quads
        List<BakedQuad> bakedQuads = new ArrayList<>();
        MutableQuad mutableQuad = null;
        for(Part part : this.parts){
            for(QuadAccess quad : part.quads.get(cullDirection)){
                // Check quad render type
                if(renderType != null){
                    RenderType quadRenderType = quad.chunkRenderType();
                    if(quadRenderType == null ? !isDefaultRenderType : quadRenderType != renderType)
                        continue;
                }

                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                if(sprite == null || pos == null){
                    bakedQuads.add(quad.toBakedQuad());
                    continue;
                }

                // Process special texture type quads
                TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                if(textureType == DefaultTextureTypes.RANDOM){
                    if(mutableQuad == null)
                        mutableQuad = MutableQuad.create();
                    mutableQuad.copyFrom(quad);
                    RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), random, sprite);
                    bakedQuads.add(mutableQuad.toBakedQuad());
                }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                    if(mutableQuad == null)
                        mutableQuad = MutableQuad.create();
                    mutableQuad.copyFrom(quad);
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                    bakedQuads.add(mutableQuad.toBakedQuad());
                }else
                    bakedQuads.add(quad.toBakedQuad());
            }
        }
        return bakedQuads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
        return this.getQuads(state, cullDirection, random, EmptyModelData.INSTANCE);
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        return true;
    }

    @Override
    public List<Pair<IBakedModel,RenderType>> getLayerModels(ItemStack stack, boolean fabulous){
        // Get default render type to use for the item
        RenderType defaultRenderType = Atlases.translucentItemSheet();
        if(stack.getItem() instanceof BlockItem && !ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(((BlockItem)stack.getItem()).getBlock().defaultBlockState(), RenderType.translucent()))
            defaultRenderType = Atlases.cutoutBlockSheet();

        // Handle each part
        List<Pair<IBakedModel,RenderType>> models = new ArrayList<>(this.parts.size());
        for(Part part : this.parts){
            // Collect quads by render type
            List<RenderType> renderTypes = new ArrayList<>(4);
            List<List<BakedQuad>> quadsByRenderType = new ArrayList<>(4);
            for(QuadAccess quad : part.quads.all()){
                // Get render type
                RenderType renderType = quad.itemRenderType();
                if(renderType == null)
                    renderType = defaultRenderType;
                // Get or quad list
                int i = renderTypes.indexOf(renderType);
                List<BakedQuad> bakedQuads;
                if(i == -1){
                    renderTypes.add(renderType);
                    bakedQuads = new ArrayList<>();
                    quadsByRenderType.add(bakedQuads);
                }else
                    bakedQuads = quadsByRenderType.get(i);
                // Add the quad to the list
                bakedQuads.add(quad.toBakedQuad());
            }

            // Create a model for each render type
            for(int i = 0; i < renderTypes.size(); i++){
                RenderType renderType = renderTypes.get(i);
                List<BakedQuad> bakedQuads = quadsByRenderType.get(i);
                models.add(Pair.of(
                    new WrappedBakedModel(this) {
                        @Override
                        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, Random random){
                            return cullDirection == null ? bakedQuads : Collections.emptyList();
                        }

                        @Override
                        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull Random random, @NotNull IModelData data){
                            return this.getQuads(state, cullDirection, random);
                        }
                    },
                    renderType
                ));
            }
        }
        return models;
    }

    @Override
    public boolean isLayered(){
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.guiLight.lightLikeBlock();
    }

    @Override
    public ItemCameraTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.EMPTY;
    }

    public static final class Part {
        private final CullableQuads quads;

        public Part(CullableQuads quads){
            this.quads = quads;
        }
    }
}
