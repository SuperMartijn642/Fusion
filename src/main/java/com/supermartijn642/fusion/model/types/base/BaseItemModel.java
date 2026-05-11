package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class BaseItemModel implements ItemModel {

    private final List<Part> parts;
    private final List<ItemTintSource> tints;
    private final boolean animated;

    public BaseItemModel(List<Part> parts, List<ItemTintSource> tints){
        this.parts = parts;
        this.tints = tints;
        this.animated = parts.stream().anyMatch(p -> p.animated);
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed){
        renderState.ensureCapacity(this.parts.size());
        renderState.appendModelIdentityElement(this);
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
        RenderType defaultRenderType = Sheets.translucentItemSheet();
        if(stack.getItem() instanceof BlockItem && ItemBlockRenderTypes.getChunkRenderType(((BlockItem)stack.getItem()).getBlock().defaultBlockState()) != ChunkSectionLayer.TRANSLUCENT)
            defaultRenderType = Sheets.cutoutBlockSheet();
        // Submit each part
        for(Part part : this.parts){
            // Create function to create layers
            Function<RenderType,ItemStackRenderState.LayerRenderState> layerConfigurer = renderType -> {
                ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
                if(tintValues != null)
                    System.arraycopy(tintValues, 0, layer.prepareTintLayers(tintValues.length), 0, tintValues.length);
                if(foilType != null)
                    layer.setFoilType(foilType);
                layer.setExtents(part.extents);
                layer.setUsesBlockLight(part.guiLight.lightLikeBlock());
                layer.setParticleIcon(part.particleSprite);
                layer.setTransform(part.transforms.getTransform(displayContext));
                layer.setRenderType(renderType);
                return layer;
            };
            // Create a layer for each render type
            List<RenderType> renderTypes = new ArrayList<>(4);
            List<ItemStackRenderState.LayerRenderState> layers = new ArrayList<>(4);
            for(QuadAccess quad : part.quads){
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
            }
        }
    }

    public static class Part {
        private final List<QuadAccess> quads;
        private final UnbakedModel.GuiLight guiLight;
        private final TextureAtlasSprite particleSprite;
        private final ItemTransforms transforms;
        private final Supplier<Vector3f[]> extents;
        private final boolean animated;

        public Part(List<QuadAccess> quads, UnbakedModel.GuiLight guiLight, TextureAtlasSprite particleSprite, ItemTransforms transforms){
            this.quads = quads;
            this.guiLight = guiLight;
            this.particleSprite = particleSprite;
            this.transforms = transforms;
            this.extents = Suppliers.memoize(() -> {
                Set<Vector3f> positions = new HashSet<>();
                for(QuadAccess quad : this.quads){
                    for(int vertex = 0; vertex < 4; vertex++){
                        positions.add(new Vector3f(quad.position(vertex)));
                    }
                }
                return positions.toArray(Vector3f[]::new);
            });

            // Check whether the quads contain animated textures
            boolean animated = false;
            for(QuadAccess quad : this.quads){
                //noinspection resource
                if(quad.sprite().contents().isAnimated()){
                    animated = true;
                    break;
                }
            }
            this.animated = animated;
        }
    }
}
