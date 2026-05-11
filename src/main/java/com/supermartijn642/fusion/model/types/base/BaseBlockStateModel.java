package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBlockStateModel;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private final List<Part> parts;
    private final ModelMaterial.Resolved particleMaterial;
    private final int materialFlags;

    public BaseBlockStateModel(List<Part> parts, ModelMaterial.Resolved particleMaterial, int materialFlags){
        this.parts = parts;
        this.particleMaterial = particleMaterial;
        this.materialFlags = materialFlags;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(ConnectingBlockStateModel.POSITION_PROPERTY, pos)
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData data){
        BlockPos pos = data.get(ConnectingBlockStateModel.POSITION_PROPERTY);

        for(Part part : this.parts){
            parts.add(new BlockStateModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    List<BakedQuad> quads = new ArrayList<>(part.quads().get(cullDirection).size());
                    MutableQuad mutableQuad = null;
                    for(QuadAccess quad : part.quads().get(cullDirection)){
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
                public Material.Baked particleMaterial(){
                    return part.particleMaterial().toBakedMaterial();
                }

                @Override
                public @BakedQuad.MaterialFlags int materialFlags(){
                    return part.materialFlags();
                }
            });
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        this.collectParts(random, parts, ModelData.EMPTY);
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial.toBakedMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    public record Part(CullableQuads quads, ModelMaterial.Resolved particleMaterial, int materialFlags) {
    }
}
