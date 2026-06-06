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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        if(stack.getItem() instanceof BlockItem && ItemBlockRenderTypes.getChunkRenderType(((BlockItem)stack.getItem()).getBlock().defaultBlockState()) != ChunkSectionLayer.TRANSLUCENT)
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
        layer.setExtents(this.extents);
        layer.setUsesBlockLight(this.guiLight.lightLikeBlock());
        layer.setParticleIcon(this.particleSprite);
        layer.setTransform(this.transforms.getTransform(displayContext));
        layer.setRenderType(defaultRenderType);

        // Process all quads
        QuadEmitter emitter = layer.emitter();
        EmittableQuad mutableQuad = null;
        for(Quad quad : this.quads){
            // Simply add quads that don't need further processing
            if(quad.processor() == null){
                quad.quad().toFrapiQuad(emitter);
                emitter.emit();
                continue;
            }

            // Extract state
            Object state = quad.processor().extractState(stack, propertyStore);

            // Create geometry key
            renderState.appendModelIdentityElement(quad.processor().createGeometryKey(state, propertyStore));

            // Create mutable quad
            if(mutableQuad == null){
                mutableQuad = EmittableQuad.create(q -> {
                    q.toFrapiQuad(emitter);
                    emitter.emit();
                });
            }
            mutableQuad.copyFrom(quad.quad());

            // Process quad
            quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, ItemQuadProcessor<Object> processor) {
    }
}
