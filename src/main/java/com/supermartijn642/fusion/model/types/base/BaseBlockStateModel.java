package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
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
    private final PropertyStore propertyStore;

    public BaseBlockStateModel(List<Part> parts, ModelMaterial.Resolved particleMaterial, int materialFlags, PropertyStore propertyStore){
        this.parts = parts;
        this.particleMaterial = particleMaterial;
        this.materialFlags = materialFlags;
        this.propertyStore = propertyStore;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts){
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(Part part : this.parts){
            // Extract state for all the textures that need processing
            //noinspection unchecked
            List<Object>[] extractStates = new List[7];
            for(Direction cullDirection : CullingHelper.cullDirections()){
                int cullIndex = CullingHelper.cullIndex(cullDirection);
                for(Quad quad : part.quads().get(cullDirection)){
                    // Ignore quads that don't need processing
                    if(quad.processor() == null)
                        continue;
                    if(extractStates[cullIndex] == null)
                        extractStates[cullIndex] = new ArrayList<>();
                    extractStates[cullIndex].add(quad.processor().extractState(level, pos, state, () -> random, propertyStore));
                }
            }

            // Create model parts
            parts.add(new BlockStateModelPart() {
                @Override
                public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                    List<Quad> quads = part.quads().get(cullDirection);
                    List<Object> states = extractStates[CullingHelper.cullIndex(cullDirection)];
                    int stateIndex = 0;

                    // Convert all quads to baked quads
                    List<BakedQuad> bakedQuads = new ArrayList<>(quads.size());
                    EmittableQuad mutableQuad = null;
                    for(Quad quad : quads){
                        // Simply add quads that don't need further processing
                        if(quad.processor() == null){
                            bakedQuads.add(quad.quad().toBakedQuad());
                            continue;
                        }

                        // Create mutable quad
                        if(mutableQuad == null)
                            mutableQuad = EmittableQuad.create(q -> bakedQuads.add(q.toBakedQuad()));
                        mutableQuad.copyFrom(quad.quad());

                        // Process special texture type quads
                        Object state = states.get(stateIndex++);
                        quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
                    }
                    return bakedQuads;
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
        this.collectParts(null, null, null, random, parts);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<Object> identity = new ArrayList<>();
        identity.add(this);
        // Add keys for all the textures that have additional processing
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(Part part : this.parts){
            for(Direction cullDirection : CullingHelper.cullDirections()){
                for(Quad quad : part.quads().get(cullDirection)){
                    // Ignore quads that don't need processing
                    if(quad.processor() == null)
                        continue;
                    // Extract state
                    Object s = quad.processor().extractState(level, pos, state, () -> random, propertyStore);
                    // Create key
                    Object key = quad.processor().createGeometryKey(s, propertyStore);
                    if(key == null)
                        return null;
                    identity.add(key);
                }
            }
        }
        return identity;
    }

    @Override
    public Material.Baked particleMaterial(){
        return this.particleMaterial.toBakedMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags(){
        return this.materialFlags;
    }

    public record Part(Quads quads, ModelMaterial.Resolved particleMaterial, int materialFlags) {
    }

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }
}
