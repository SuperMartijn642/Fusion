package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class BaseItemModel implements ItemModel {

    private final List<Quad> quads;
    private final ModelPredicate conditions;
    private final PropertyStore propertyStore;
    private final UnbakedModel.GuiLight guiLight;
    private final TextureAtlasSprite particleSprite;
    private final ItemTransforms transforms;
    private final List<ItemTintSource> tints;

    public BaseItemModel(List<Quad> quads, ModelPredicate conditions, PropertyStore propertyStore, UnbakedModel.GuiLight guiLight, TextureAtlasSprite particleSprite, ItemTransforms transforms, List<ItemTintSource> tints){
        this.quads = quads;
        this.conditions = conditions;
        this.propertyStore = propertyStore;
        this.guiLight = guiLight;
        this.particleSprite = particleSprite;
        this.transforms = transforms;
        this.tints = tints;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed){
        // Check conditions
        if(this.conditions != null && !this.conditions.testForItem(stack))
            return;

        // Compute tint values
        int[] tintValues;
        if(!this.tints.isEmpty()){
            tintValues = new int[this.tints.size()];
            for(int i = 0; i < this.tints.size(); i++)
                tintValues[i] = this.tints.get(i).calculate(stack, level, owner);
        }else
            tintValues = null;
        // Get foil type
        ItemStackRenderState.FoilType foilType = stack.hasFoil() ?
            BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD :
            null;
        // Get default render type to use for the item
        RenderType defaultRenderType;
        if(stack.getItem() instanceof BlockItem && ItemBlockRenderTypes.getChunkRenderType(((BlockItem)stack.getItem()).getBlock().defaultBlockState()) != RenderType.translucent())
            defaultRenderType = Sheets.cutoutBlockSheet();
        else
            defaultRenderType = Sheets.translucentItemSheet();
        // Submit each part
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Create and configure layer
        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        if(tintValues != null)
            System.arraycopy(tintValues, 0, layer.prepareTintLayers(tintValues.length), 0, tintValues.length);
        if(foilType != null)
            layer.setFoilType(foilType);
        layer.setupBlockModel(new BakedModel() {
            @Override
            public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier){
                // Process all quads
                EmittableQuad mutableQuad = null;
                for(Quad quad : BaseItemModel.this.quads){
                    // Simply add quads that don't need further processing
                    if(quad.processor() == null){
                        quad.quad().toFrapiQuad(emitter);
                        emitter.emit();
                        continue;
                    }

                    // Create mutable quad
                    if(mutableQuad == null){
                        mutableQuad = EmittableQuad.create(q -> {
                            q.toFrapiQuad(emitter);
                            emitter.emit();
                        });
                    }
                    mutableQuad.copyFrom(quad.quad());

                    // Process quad
                    Object state = quad.processor().extractState(stack, propertyStore);
                    quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
                }
            }

            @Override
            public boolean isVanillaAdapter(){
                return false;
            }

            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource randomSource){
                if(cullDirection != null)
                    return List.of();

                // Convert all quads to baked quads
                List<BakedQuad> bakedQuads = new ArrayList<>();
                EmittableQuad mutableQuad = null;
                for(Quad quad : BaseItemModel.this.quads){
                    // Simply add quads that don't need further processing
                    if(quad.processor() == null){
                        bakedQuads.add(quad.quad().toBakedQuad());
                        continue;
                    }

                    // Create mutable quad
                    if(mutableQuad == null)
                        mutableQuad = EmittableQuad.create(q -> bakedQuads.add(q.toBakedQuad()));
                    mutableQuad.copyFrom(quad.quad());

                    // Process quad
                    Object s = quad.processor().extractState(stack, propertyStore);
                    quad.processor().processQuad(mutableQuad, quad.sprite(), s, propertyStore);
                }
                return bakedQuads;
            }

            @Override
            public boolean useAmbientOcclusion(){
                return true;
            }

            @Override
            public boolean isGui3d(){
                return true;
            }

            @Override
            public boolean usesBlockLight(){
                return BaseItemModel.this.guiLight.lightLikeBlock();
            }

            @Override
            public TextureAtlasSprite getParticleIcon(){
                return BaseItemModel.this.particleSprite;
            }

            @Override
            public ItemTransforms getTransforms(){
                return BaseItemModel.this.transforms;
            }
        }, defaultRenderType);
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, ItemQuadProcessor<Object> processor) {
    }
}
