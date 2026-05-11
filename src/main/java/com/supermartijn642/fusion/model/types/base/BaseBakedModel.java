package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final boolean ambientOcclusion;
    private final boolean isGui3d;
    private final ItemCameraTransforms transforms;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, boolean ambientOcclusion, boolean isGui3d, ItemCameraTransforms transforms){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.ambientOcclusion = ambientOcclusion;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        BlockRenderContext blockRenderContext = FusionClient.BLOCK_RENDER_CONTEXT.get();
        BlockPos pos = blockRenderContext == null ? null : blockRenderContext.pos();

        // Get whether the giving render type is the default render type
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean isDefaultRenderType;
        if(renderType != null){
            BlockRenderLayer defaultRenderType = state == null ?
                BlockRenderLayer.SOLID :
                state.getBlock().getBlockLayer();
            isDefaultRenderType = renderType == defaultRenderType;
        }else
            isDefaultRenderType = true;

        // Collect quads
        List<BakedQuad> bakedQuads = new ArrayList<>();
        Random random = null;
        MutableQuad mutableQuad = null;
        for(Part part : this.parts){
            for(QuadAccess quad : part.quads.get(cullDirection)){
                // Check quad render type
                if(renderType != null){
                    BlockRenderLayer quadRenderType = quad.renderLayer();
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
                    if(random == null)
                        random = new Random();
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
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer){
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.particleSprite;
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    @Override
    public ItemOverrideList getOverrides(){
        return ItemOverrideList.NONE;
    }

    public static final class Part {
        private final CullableQuads quads;

        public Part(CullableQuads quads){
            this.quads = quads;
        }
    }
}
