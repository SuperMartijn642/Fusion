package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
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

    private final List<Part> parts;
    private final List<ItemTintSource> tints;
    private final ModelTransform transformation;
    private final boolean animated;

    public BaseItemModel(List<Part> parts, List<ItemTintSource> tints, ModelTransform transformation){
        this.parts = parts;
        this.tints = tints;
        this.transformation = transformation;
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
        // Submit each part
        for(Part part : this.parts){
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(tintValues != null)
                layer.tintLayers().addAll(tintValues);
            if(foilType != null)
                layer.setFoilType(foilType);
            layer.setExtents(part.extents);
            layer.setLocalTransform(this.transformation.matrix());
            layer.setUsesBlockLight(part.guiLight.lightLikeBlock());
            layer.setParticleMaterial(part.particleMaterial.toBakedMaterial());
            layer.setItemTransform(part.transforms.getTransform(displayContext));
            QuadEmitter emitter = layer.emitter();
            for(QuadAccess quad : part.quads){
                quad.toFrapiQuad(emitter);
                emitter.emit();
            }
        }
    }

    public static class Part {
        private final List<QuadAccess> quads;
        private final UnbakedModel.GuiLight guiLight;
        private final ModelMaterial.Resolved particleMaterial;
        private final ItemTransforms transforms;
        private final Supplier<Vector3fc[]> extents;
        private final boolean animated;

        public Part(List<QuadAccess> quads, UnbakedModel.GuiLight guiLight, ModelMaterial.Resolved particleMaterial, ItemTransforms transforms){
            this.quads = quads;
            this.guiLight = guiLight;
            this.particleMaterial = particleMaterial;
            this.transforms = transforms;
            this.extents = () -> {
                Set<Vector3fc> positions = new HashSet<>();
                for(QuadAccess quad : this.quads){
                    for(int vertex = 0; vertex < 4; vertex++){
                        positions.add(quad.position(vertex));
                    }
                }
                return positions.toArray(Vector3fc[]::new);
            };

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
