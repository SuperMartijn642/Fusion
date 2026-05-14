package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
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
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBlockStateModel implements BlockStateModel {

    private static final ModelProperty<RenderData> RENDER_DATA = new ModelProperty<>();

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

    private RenderData getRenderData(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state){
        // Create random supplier
        Supplier<RandomSource> randomSupplier = Suppliers.memoize(() -> {
            long seed = state == null ?
                pos == null ? 0 : pos.asLong() :
                pos == null ? state.getSeed(BlockPos.ZERO) : state.getSeed(pos);
            return RandomSource.createThreadLocalInstance(seed);
        });

        // For each part, collect whether the part's conditions are met and collect texture states for the part
        boolean[] partConditions = new boolean[this.parts.size()];
        //noinspection unchecked
        List<Object>[][] combinedStates = new List[this.parts.size()][];
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Check part condition
            if(part.conditions != null && !part.conditions.testForBlock(level, pos, state))
                continue;
            partConditions[i] = true;

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
                    extractStates[cullIndex].add(quad.processor().extractState(level, pos, state, randomSupplier, propertyStore));
                }
            }
            combinedStates[i] = extractStates;
        }
        return new RenderData(partConditions, combinedStates, propertyStore);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData){
        return ModelData.builder()
            .with(RENDER_DATA, this.getRenderData(level, pos, state))
            .build();
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts, ModelData modelData){
        // Read render data
        RenderData renderData = modelData.get(RENDER_DATA);
        if(renderData == null)
            renderData = this.getRenderData(null, null, null);
        PropertyStore propertyStore = renderData.propertyStore;

        // Handle each part
        for(int i = 0; i < this.parts.size(); i++){
            Part part = this.parts.get(i);
            // Check part condition
            if(!renderData.partConditions[i])
                continue;

            // Get texture states
            List<Object>[] extractStates = renderData.statesForPart(i);

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

    public record Part(Quads quads, ModelPredicate conditions, ModelMaterial.Resolved particleMaterial, int materialFlags) {
    }

    public record Quads(List<Quad>[] quads) {
        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, BlockStateQuadProcessor<Object> processor) {
    }

    private record RenderData(boolean[] partConditions, List<Object>[][] combinedTextureStates, PropertyStore propertyStore) {
        List<Object>[] statesForPart(int index){
            return this.combinedTextureStates[index];
        }
    }
}
