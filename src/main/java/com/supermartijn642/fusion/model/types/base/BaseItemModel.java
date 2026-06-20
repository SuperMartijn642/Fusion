package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private final Supplier<Vector3f[]> extents;
    private final boolean animated;

    public BaseItemModel(List<Quad> quads, ModelPredicate conditions, PropertyStore propertyStore, UnbakedModel.GuiLight guiLight, TextureAtlasSprite particleSprite, ItemTransforms transforms, List<ItemTintSource> tints){
        this.quads = quads;
        this.conditions = conditions;
        this.propertyStore = propertyStore;
        this.guiLight = guiLight;
        this.particleSprite = particleSprite;
        this.transforms = transforms;
        this.tints = tints;
        this.extents = () -> {
            Set<Vector3f> positions = new HashSet<>();
            for(Quad quad : this.quads){
                for(int vertex = 0; vertex < 4; vertex++){
                    positions.add(new Vector3f(quad.quad().position(vertex)));
                }
            }
            return positions.toArray(Vector3f[]::new);
        };

        // Check whether the quads contain animated textures
        boolean animated = false;
        for(Quad quad : this.quads){
            //noinspection resource
            if(quad.quad().sprite().contents().isAnimated()){
                animated = true;
                break;
            }
        }
        this.animated = animated;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed){
        renderState.appendModelIdentityElement(this);

        // Check conditions
        if(this.conditions != null){
            if(!this.conditions.testForItem(stack)){
                renderState.appendModelIdentityElement(false);
                return;
            }
            renderState.appendModelIdentityElement(true);
        }

        // Set animated
        if(this.animated)
            renderState.setAnimated();
        // Compute tint values
        int[] tintValues;
        if(!this.tints.isEmpty()){
            tintValues = new int[this.tints.size()];
            for(int i = 0; i < this.tints.size(); i++){
                int tint = this.tints.get(i).calculate(stack, level, owner == null ? null : owner.asLivingEntity());
                tintValues[i] = tint;
                renderState.appendModelIdentityElement(tint);
            }
        }else
            tintValues = null;
        // Get foil type
        ItemStackRenderState.FoilType foilType;
        if(stack.hasFoil()){
            foilType = BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD;
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }else
            foilType = null;
        // Get default render type to use for the item
        RenderType defaultRenderType;
        if(stack.getItem() instanceof BlockItem && !ItemBlockRenderTypes.getRenderLayers(((BlockItem)stack.getItem()).getBlock().defaultBlockState()).contains(ChunkSectionLayer.TRANSLUCENT))
            defaultRenderType = Sheets.cutoutBlockSheet();
        else
            defaultRenderType = Sheets.translucentItemSheet();
        // Submit each part
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Create function to create layers
        Function<RenderType,ItemStackRenderState.LayerRenderState> layerConfigurer = renderType -> {
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(tintValues != null)
                System.arraycopy(tintValues, 0, layer.prepareTintLayers(tintValues.length), 0, tintValues.length);
            if(foilType != null)
                layer.setFoilType(foilType);
            layer.setExtents(this.extents);
            layer.setUsesBlockLight(this.guiLight.lightLikeBlock());
            layer.setParticleIcon(this.particleSprite);
            layer.setTransform(this.transforms.getTransform(displayContext));
            layer.setRenderType(renderType);
            return layer;
        };

        // Create function for submitting quads
        List<RenderType> renderTypes = new ArrayList<>(4);
        List<ItemStackRenderState.LayerRenderState> layers = new ArrayList<>(4);
        Consumer<QuadAccess> submitter = quad -> {
            // Get render type
            RenderType renderType = quad.itemRenderType();
            if(renderType == null)
                renderType = defaultRenderType;
            // Get or create layer
            int i = renderTypes.indexOf(renderType);
            ItemStackRenderState.LayerRenderState layer;
            if(i == -1){
                renderTypes.add(renderType);
                layer = layerConfigurer.apply(renderType);
                layers.add(layer);
            }else
                layer = layers.get(i);
            // Add the quad to the layer
            layer.prepareQuadList().add(quad.toBakedQuad());
        };

        // Process all quads
        EmittableQuad mutableQuad = null;
        for(Quad quad : this.quads){
            // Simply add quads that don't need further processing
            if(quad.processor() == null){
                submitter.accept(quad.quad());
                continue;
            }

            // Extract state
            Object state = quad.processor().extractState(stack, propertyStore);

            // Create geometry key
            renderState.appendModelIdentityElement(quad.processor().createGeometryKey(state, propertyStore));

            // Create mutable quad
            if(mutableQuad == null)
                mutableQuad = EmittableQuad.create(submitter::accept);
            mutableQuad.copyFrom(quad.quad());

            // Process quad
            quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, ItemQuadProcessor<Object> processor) {
    }
}
