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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;

    public BaseBlockStateModel(List<Part> parts, TextureAtlasSprite particleSprite){
        this.parts = parts;
        this.particleSprite = particleSprite;
    }

    @Override
    public void collectParts(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, RandomSource random, List<BlockModelPart> parts){
        // Get the default render type to use
        //noinspection deprecation
        ChunkSectionLayer defaultRenderType = state == null ?
            ChunkSectionLayer.SOLID :
            ItemBlockRenderTypes.getChunkRenderType(state);

        // Handle each part
        MutableQuad mutableQuad = null;
        for(Part part : this.parts){
            // Group quads by render type
            Map<ChunkSectionLayer,CullableQuads.Builder> quadsByRenderType = new EnumMap<>(ChunkSectionLayer.class);
            BiConsumer<QuadAccess,Direction> submitter = (quad, cullDirection) -> {
                // Get the quad's render type
                ChunkSectionLayer renderType = quad.chunkLayer();
                if(renderType == null)
                    renderType = defaultRenderType;
                // Add the baked quad
                quadsByRenderType.computeIfAbsent(renderType, r -> CullableQuads.builder())
                    .add(cullDirection, quad);
            };

            // Add all quads
            for(Direction cullDirection : ConnectingBlockStateModel.CULL_DIRECTIONS){
                for(QuadAccess quad : part.quads().get(cullDirection)){
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(sprite == null || pos == null){
                        submitter.accept(quad, cullDirection);
                        continue;
                    }

                    // Process special texture type quads
                    TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                    if(textureType == DefaultTextureTypes.RANDOM){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), random, sprite);
                        submitter.accept(mutableQuad.createCopy(), cullDirection);
                    }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                        submitter.accept(mutableQuad.createCopy(), cullDirection);
                    }else
                        submitter.accept(quad, cullDirection);
                }
            }

            // Create a model part for each render type
            for(ChunkSectionLayer renderType : quadsByRenderType.keySet()){
                CullableQuads quads = quadsByRenderType.get(renderType).build();
                parts.add(new BlockModelPart() {
                    @Override
                    public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                        List<QuadAccess> q = quads.get(cullDirection);
                        List<BakedQuad> bakedQuads = new ArrayList<>(q.size());
                        for(QuadAccess quad : q)
                            bakedQuads.add(quad.toBakedQuad());
                        return bakedQuads;
                    }

                    @Override
                    public boolean useAmbientOcclusion(){
                        return true;
                    }

                    @Override
                    public TextureAtlasSprite particleIcon(){
                        return part.particleSprite();
                    }

                    @Override
                    public ChunkSectionLayer getRenderType(BlockState state){
                        return renderType;
                    }
                });
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts){
        this.collectParts(null, null, null, random, parts);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        return null;
    }

    @Override
    public TextureAtlasSprite particleIcon(){
        return this.particleSprite;
    }

    public record Part(CullableQuads quads, TextureAtlasSprite particleSprite) {
    }
}
