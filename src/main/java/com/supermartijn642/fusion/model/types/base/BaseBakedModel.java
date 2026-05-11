package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemTransforms transforms;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, BlockModel.GuiLight guiLight, boolean isGui3d, ItemTransforms transforms){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(ConnectingBakedModel.POSITION_PROPERTY, pos)
            .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random, ModelData modelData, @Nullable RenderType renderType){
        BlockPos pos = modelData.get(ConnectingBakedModel.POSITION_PROPERTY);

        // Get whether the giving render type is the default render type
        boolean isDefaultRenderType;
        if(renderType != null){
            //noinspection deprecation
            RenderType defaultRenderType = state == null ?
                RenderType.solid() :
                ItemBlockRenderTypes.getChunkRenderType(state);
            isDefaultRenderType = renderType == defaultRenderType;
        }else
            isDefaultRenderType = true;

        // Collect quads
        List<BakedQuad> bakedQuads = new ArrayList<>();
        MutableQuad mutableQuad = null;
        for(Part part : this.parts){
            for(QuadAccess quad : part.quads().get(cullDirection)){
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
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return this.getQuads(state, cullDirection, random, ModelData.EMPTY, null);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        return ChunkRenderTypeSet.all();
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous){
        // Get default render type to use for the item
        RenderType defaultRenderType = Sheets.translucentItemSheet();
        if(stack.getItem() instanceof BlockItem && ItemBlockRenderTypes.getChunkRenderType(((BlockItem)stack.getItem()).getBlock().defaultBlockState()) != RenderType.translucent())
            defaultRenderType = Sheets.cutoutBlockSheet();

        // Handle each part
        List<BakedModel> models = new ArrayList<>(this.parts.size());
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

            // Create model
            models.add(new WrappedBakedModel(this) {
                @Override
                public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType){
                    if(renderType == null){
                        List<BakedQuad> combined = new ArrayList<>();
                        for(List<BakedQuad> quads : quadsByRenderType)
                            combined.addAll(quads);
                        return combined;
                    }
                    int i = renderTypes.indexOf(renderType);
                    return i == -1 ? List.of() : quadsByRenderType.get(i);
                }

                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
                    return this.getQuads(state, cullDirection, random, ModelData.EMPTY, null);
                }

                @Override
                public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous){
                    return renderTypes;
                }
            });
        }
        return models;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return true; // Ambient occlusion is handled by quads themselves
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
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    public record Part(CullableQuads quads) {
    }
}
