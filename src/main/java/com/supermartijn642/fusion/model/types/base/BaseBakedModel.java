package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.quad.MutableQuad;
import com.supermartijn642.fusion.model.quad.MutableQuadImpl;
import com.supermartijn642.fusion.model.quad.QuadAccess;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BlockStateModel {

    public static final ModelProperty<BlockPos> POSITION_PROPERTY = new ModelProperty<>();

    private final List<TaggedBakedQuad>[] mesh;
    private final List<SpriteInstance> sprites;
    private final boolean hasSpecialQuads;
    private final Boolean hasAmbientOcclusion;
    private final Material.Baked particleIcon;
    private final int materialFlags;

    public BaseBakedModel(List<BaseModelQuad> quads, Boolean hasAmbientOcclusion, Material.Baked particleIcon){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.particleIcon = particleIcon;

        // Create block mesh
        //noinspection unchecked
        List<TaggedBakedQuad>[] mesh = new List[7];
        HashMap<SpriteInstance,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        int materialFlags = 0;
        for(BaseModelQuad quad : quads){
            // Tag quads that need additional processing
            int spriteIndex = -1;
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                // Give each sprite a unique index
                spriteIndex = sprites.computeIfAbsent(quad.spriteInstance(), o -> sprites.size());
                hasSpecialQuads = true;
            }
            TaggedBakedQuad taggedQuad = new TaggedBakedQuad(quad.quad(), quad.textureType(), spriteIndex);
            // Add the block quads
            int cullIndex = cullIndex(quad.cullDirection());
            if(mesh[cullIndex] == null)
                mesh[cullIndex] = new ArrayList<>();
            mesh[cullIndex].add(taggedQuad);
            // Update material flags
            if(quad.quad().chunkLayer().translucent())
                materialFlags |= BakedQuad.FLAG_TRANSLUCENT;
            if(quad.quad().sprite().contents().isAnimated())
                materialFlags |= BakedQuad.FLAG_ANIMATED;
        }
        this.mesh = mesh;
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;
        this.materialFlags = materialFlags;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData data){
        parts.add(new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                return BaseBakedModel.this.getQuads(cullDirection, random, data);
            }

            @Override
            public boolean useAmbientOcclusion(){
                return BaseBakedModel.this.hasAmbientOcclusion != Boolean.FALSE;
            }

            @Override
            public Material.Baked particleMaterial(){
                return BaseBakedModel.this.particleIcon;
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return BaseBakedModel.this.materialFlags;
            }
        });
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY);
    }

    private List<BakedQuad> getQuads(@Nullable Direction cullDirection, RandomSource random, ModelData data){
        List<TaggedBakedQuad> quads = this.mesh[cullIndex(cullDirection)];
        if(quads == null)
            return Collections.emptyList();

        // If there's no special quads, just return the quads as is
        if(!this.hasSpecialQuads){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.quad.toBakedQuad());
            return bakedQuads;
        }

        // Get the position from the model data
        BlockPos pos = data.get(POSITION_PROPERTY);
        // If the position is absent, just return the quads
        if(pos == null){
            List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
            for(TaggedBakedQuad quad : quads)
                bakedQuads.add(quad.quad.toBakedQuad());
            return bakedQuads;
        }

        // Push a transform which maps any connecting texture quads to the correct uv
        ArrayList<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
        MutableQuad mutableQuad = new MutableQuadImpl();
        for(TaggedBakedQuad quad : quads){
            // Process special texture type quads
            if(quad.textureType == DefaultTextureTypes.RANDOM || quad.textureType == DefaultTextureTypes.CONTINUOUS){
                // Get the sprite
                SpriteInstance sprite = this.sprites.get(quad.spriteIndex);

                mutableQuad.copyFrom(quad.quad);
                if(quad.textureType == DefaultTextureTypes.RANDOM)
                    // Handle random texture type
                    RandomTextureType.processQuad(mutableQuad, pos, quad.quad.facing(), random, sprite);
                else
                    // Handle continuous texture type
                    ContinuousTextureType.processQuad(mutableQuad, pos, quad.quad.facing(), sprite);
                bakedQuads.add(mutableQuad.toBakedQuad());
            }else
                bakedQuads.add(quad.quad.toBakedQuad());
        }
        return bakedQuads;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data){
        if(!this.hasSpecialQuads)
            return ModelData.EMPTY;
        return ModelData.builder().with(POSITION_PROPERTY, pos).build();
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleIcon;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    private static int cullIndex(Direction cullDirection){
        return cullDirection == null ? 0 : cullDirection.ordinal() + 1;
    }

    private static class TaggedBakedQuad {
        final QuadAccess quad;
        final TextureType<?,?> textureType;
        final int spriteIndex;

        private TaggedBakedQuad(QuadAccess quad, TextureType<?,?> textureType, int spriteIndex){
            this.quad = quad;
            this.textureType = textureType;
            this.spriteIndex = spriteIndex;
        }
    }
}
