package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
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
import java.util.function.Predicate;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private final Quads quads;
    private final ModelPredicate conditions;
    private final ModelMaterial.Resolved particleMaterial;
    private final PropertyStore propertyStore;
    private final int materialFlags;

    public BaseBlockStateModel(Quads quads, ModelPredicate conditions, ModelMaterial.Resolved particleMaterial, PropertyStore propertyStore){
        this.quads = quads;
        this.conditions = conditions;
        this.particleMaterial = particleMaterial;
        this.propertyStore = propertyStore;

        // Find material flags
        int materialFlags = 0;
        for(Direction cullDirection : CullingHelper.cullDirections()){
            for(BaseBlockStateModel.Quad quad : quads.get(cullDirection)){
                if(quad.quad().chunkLayer().translucent())
                    materialFlags |= 1;
                else if(quad.quad().sprite().contents().isAnimated())
                    materialFlags |= 2;
            }
        }
        this.materialFlags = materialFlags;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForBlockState(level, pos, state))
            return;

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Emit quads for all cull directions
        for(Direction cullDirection : CullingHelper.cullDirections()){
            // Skip direction if it doesn't pass the cull test
            if(cullTest.test(cullDirection))
                continue;

            EmittableQuad mutableQuad = EmittableQuad.create(q -> {
                emitter.cullFace(cullDirection);
                emitter.shadeMode(ShadeMode.VANILLA);
                q.toFrapiQuad(emitter);
                emitter.emit();
            });
            for(Quad quad : this.quads.get(cullDirection)){
                // Copy quad properties
                mutableQuad.copyFrom(quad.quad());

                // Simply add quads that don't need further processing
                if(quad.processor() == null){
                    mutableQuad.emit();
                    continue;
                }

                // Process special texture type quads
                Object s = quad.processor().extractState(level, pos, state, () -> random, propertyStore);
                quad.processor().processQuad(mutableQuad, quad.sprite(), s, propertyStore);
            }
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForBlockState(null, null, null))
            return;

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Extract state for all the textures that need processing
        //noinspection unchecked
        List<Object>[] extractStates = new List[7];
        for(Direction cullDirection : CullingHelper.cullDirections()){
            int cullIndex = CullingHelper.cullIndex(cullDirection);
            for(Quad quad : this.quads.get(cullDirection)){
                // Ignore quads that don't need processing
                if(quad.processor() == null)
                    continue;
                if(extractStates[cullIndex] == null)
                    extractStates[cullIndex] = new ArrayList<>();
                extractStates[cullIndex].add(quad.processor().extractState(null, null, null, () -> random, propertyStore));
            }
        }

        // Create model parts
        parts.add(new BlockStateModelPart() {
            @Override
            public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
                List<Quad> quads = BaseBlockStateModel.this.quads.get(cullDirection);
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
                return BaseBlockStateModel.this.particleMaterial.toBakedMaterial();
            }

            @Override
            public @BakedQuad.MaterialFlags int materialFlags(){
                return BaseBlockStateModel.this.materialFlags;
            }
        });
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random){
        List<Object> identity = new ArrayList<>();
        identity.add(this);
        // Check condition
        if(this.conditions != null){
            if(!this.conditions.testForBlockState(level, pos, state)){
                identity.add(false);
                return identity;
            }
            identity.add(true);
        }
        // Add keys for all the textures that have additional processing
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(Direction cullDirection : CullingHelper.cullDirections()){
            for(Quad quad : this.quads.get(cullDirection)){
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

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }
}
