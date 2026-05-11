package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBlockStateModel;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    public static final Collection<ChunkSectionLayer> ALL_CHUNK_RENDER_TYPES = EnumSet.allOf(ChunkSectionLayer.class);

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;

    public BaseBlockStateModel(List<Part> parts, TextureAtlasSprite particleSprite){
        this.parts = parts;
        this.particleSprite = particleSprite;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(ConnectingBlockStateModel.POSITION_PROPERTY, pos)
            .with(ConnectingBlockStateModel.STATE_PROPERTY, state)
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts, ModelData data, @Nullable ChunkSectionLayer renderType){
        BlockPos pos = data.get(ConnectingBlockStateModel.POSITION_PROPERTY);
        BlockState state = data.get(ConnectingBlockStateModel.STATE_PROPERTY);

        // Get whether the giving render type is the default render type
        boolean isDefaultRenderType;
        if(renderType != null){
            //noinspection deprecation
            ChunkSectionLayer defaultRenderType = state == null ?
                ChunkSectionLayer.SOLID :
                ItemBlockRenderTypes.getChunkRenderType(state);
            isDefaultRenderType = renderType == defaultRenderType;
        }else
            isDefaultRenderType = true;

        // Handle each part
        for(Part part : this.parts){
            parts.add(new BlockModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    List<BakedQuad> quads = new ArrayList<>(part.quads().get(cullDirection).size());
                    MutableQuad mutableQuad = null;
                    for(QuadAccess quad : part.quads().get(cullDirection)){
                        // Check quad render type
                        if(renderType != null){
                            ChunkSectionLayer quadRenderType = quad.chunkLayer();
                            if(quadRenderType == null ? !isDefaultRenderType : quadRenderType != renderType)
                                continue;
                        }

                        // Get the sprite instance
                        SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                        if(sprite == null || pos == null){
                            quads.add(quad.toBakedQuad());
                            continue;
                        }

                        // Process special texture type quads
                        TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                        if(textureType == DefaultTextureTypes.RANDOM){
                            if(mutableQuad == null)
                                mutableQuad = MutableQuad.create();
                            mutableQuad.copyFrom(quad);
                            RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), random, sprite);
                            quads.add(mutableQuad.toBakedQuad());
                        }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                            if(mutableQuad == null)
                                mutableQuad = MutableQuad.create();
                            mutableQuad.copyFrom(quad);
                            ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                            quads.add(mutableQuad.toBakedQuad());
                        }else
                            quads.add(quad.toBakedQuad());
                    }
                    return quads;
                }

                @Override
                public boolean useAmbientOcclusion(){
                    return true;
                }

                @Override
                public TextureAtlasSprite particleIcon(){
                    return part.particleSprite();
                }
            });
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY, null);
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data){
        return ALL_CHUNK_RENDER_TYPES;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }

    public record Part(CullableQuads quads, TextureAtlasSprite particleSprite) {
    }
}
