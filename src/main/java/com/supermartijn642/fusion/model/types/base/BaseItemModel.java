package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

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
    private final ModelTransform transformation;
    private final UnbakedModel.GuiLight guiLight;
    private final ModelMaterial.Resolved particleMaterial;
    private final ItemTransforms transforms;
    private final List<ItemTintSource> tints;
    private final Supplier<Vector3fc[]> extents;
    private final boolean animated;

    public BaseItemModel(List<Quad> quads, ModelPredicate conditions, PropertyStore propertyStore, ModelTransform transformation, UnbakedModel.GuiLight guiLight, ModelMaterial.Resolved particleMaterial, ItemTransforms transforms, List<ItemTintSource> tints){
        this.quads = quads;
        this.conditions = conditions;
        this.propertyStore = propertyStore;
        this.transformation = transformation;
        this.guiLight = guiLight;
        this.particleMaterial = particleMaterial;
        this.transforms = transforms;
        this.tints = tints;
        this.extents = () -> {
            Set<Vector3fc> positions = new HashSet<>();
            for(Quad quad : this.quads){
                for(int vertex = 0; vertex < 4; vertex++){
                    positions.add(quad.quad().position(vertex));
                }
            }
            return positions.toArray(Vector3fc[]::new);
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

        // Check part conditions
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
        IntList tintValues = null;
        if(!this.tints.isEmpty()){
            int[] arr = new int[this.tints.size()];
            for(int i = 0; i < this.tints.size(); i++){
                int tint = this.tints.get(i).calculate(stack, level, owner == null ? null : owner.asLivingEntity());
                arr[i] = tint;
                renderState.appendModelIdentityElement(tint);
            }
            tintValues = IntList.of(arr);
        }
        // Get foil type
        ItemStackRenderState.FoilType foilType = null;
        if(stack.hasFoil()){
            foilType = CuboidItemModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD;
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }

        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Create and configure layer
        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        if(tintValues != null)
            layer.tintLayers().addAll(tintValues);
        if(foilType != null)
            layer.setFoilType(foilType);
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transformation.matrix());
        layer.setUsesBlockLight(this.guiLight.lightLikeBlock());
        layer.setParticleMaterial(this.particleMaterial.toBakedMaterial());
        layer.setItemTransform(this.transforms.getTransform(displayContext));

        // Process all quads
        List<BakedQuad> bakedQuads = layer.prepareQuadList();
        EmittableQuad mutableQuad = null;
        for(Quad quad : this.quads){
            // Simply add quads that don't need further processing
            if(quad.processor() == null){
                bakedQuads.add(quad.quad().toBakedQuad());
                continue;
            }

            // Extract state
            Object state = quad.processor().extractState(stack, propertyStore);

            // Create geometry key
            renderState.appendModelIdentityElement(quad.processor().createGeometryKey(state, propertyStore));

            // Create mutable quad
            if(mutableQuad == null)
                mutableQuad = EmittableQuad.create(q -> bakedQuads.add(q.toBakedQuad()));
            mutableQuad.copyFrom(quad.quad());

            // Process quad
            quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, ItemQuadProcessor<Object> processor) {
    }
}
