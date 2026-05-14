package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel {

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemTransforms transforms;
    private final PropertyStore propertyStore;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, BlockModel.GuiLight guiLight, boolean isGui3d, ItemTransforms transforms, PropertyStore propertyStore){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
        this.propertyStore = propertyStore;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Emit all quads
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            // Check part condition
            if(part.conditions != null && !part.conditions.testForBlock(level, pos, state))
                continue;

            // Process all quads
            for(Direction cullDirection : CullingHelper.cullDirections()){
                // Skip direction if it doesn't pass the cull test
                if(context.isFaceCulled(cullDirection))
                    continue;

                emitter.cullFace(cullDirection);

                EmittableQuad mutableQuad = null;
                for(Quad quad : part.quads().get(cullDirection)){
                    // Simply add quads that don't need further processing
                    if(quad.processor() == null){
                        quad.quad().toFrapiQuad(emitter);
                        emitter.emit();
                        continue;
                    }

                    // Create mutable quad
                    if(mutableQuad == null)
                        mutableQuad = EmittableQuad.create(q -> {
                            q.toFrapiQuad(emitter);
                            emitter.emit();
                        });
                    mutableQuad.copyFrom(quad.quad());

                    // Process special texture type quads
                    Object s = quad.processor().extractState(level, pos, state, randomSupplier, propertyStore);
                    quad.processor().processQuad(mutableQuad, quad.sprite(), s, propertyStore);
                }
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Emit all quads
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            // Check part condition
            if(part.conditions != null && !part.conditions.testForItem(stack))
                continue;

            // Process all quads
            EmittableQuad mutableQuad = null;
            for(Quad quad : part.quads().all()){
                // Simply add quads that don't need further processing
                if(quad.processor() == null){
                    quad.quad().toFrapiQuad(emitter);
                    emitter.emit();
                    continue;
                }

                // Create mutable quad
                if(mutableQuad == null)
                    mutableQuad = EmittableQuad.create(q -> {
                        q.toFrapiQuad(emitter);
                        emitter.emit();
                    });
                mutableQuad.copyFrom(quad.quad());

                // Process special texture type quads
                Object s = quad.processor().extractState(stack, randomSupplier, propertyStore);
                quad.processor().processQuad(mutableQuad, quad.sprite(), s, propertyStore);
            }
        }
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Check whether we should use block context
        boolean hasBlockContext = state != null;

        // Convert all quads to baked quads
        List<BakedQuad> bakedQuads = new ArrayList<>();
        EmittableQuad mutableQuad = null;
        for(Part part : this.parts){
            // Check part condition
            if(part.conditions != null && !part.conditions.testForBlock(null, null, state))
                continue;

            // Process quads
            for(Quad quad : part.quads().get(cullDirection)){
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
                Object s = hasBlockContext ?
                    quad.processor().extractState(null, null, state, () -> random, propertyStore) :
                    quad.processor().extractState(() -> random, propertyStore);
                quad.processor().processQuad(mutableQuad, quad.sprite(), s, propertyStore);
            }
        }
        return bakedQuads;
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

    @Override
    public ItemOverrides getOverrides(){
        return ItemOverrides.EMPTY;
    }

    public record Part(Quads quads, ModelPredicate conditions) {
    }

    public record Quads(List<Quad>[] quads, List<Quad> all) {
        public static Quads create(List<Quad>[] quads){
            List<Quad> combined = new ArrayList<>();
            for(List<Quad> l : quads)
                combined.addAll(l);
            return new Quads(quads, List.copyOf(combined));
        }

        List<Quad> get(Direction cullDirection){
            return this.quads[CullingHelper.cullIndex(cullDirection)];
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, QuadProcessor<Object> processor) {
    }
}
