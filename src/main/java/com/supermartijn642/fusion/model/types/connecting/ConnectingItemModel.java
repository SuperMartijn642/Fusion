package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderTypeHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class ConnectingItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final List<Pair<Optional<RenderType>,List<BakedQuad>>> mesh;
    private final Supplier<Vector3f[]> extents;
    private final boolean animated;

    public ConnectingItemModel(List<ItemTintSource> tints, List<ConnectingModelQuad> quads, ModelRenderProperties properties, RenderType forgeRenderType){
        this.tints = tints;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        Map<Optional<RenderType>,List<BakedQuad>> mesh = new LinkedHashMap<>();
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = quad.hasConnectingTexture() ? ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount() : 0;
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                mutableQuad.fillFromBakedQuad(quad.bakedQuad());
                mutableQuad.emissive(quad.emissive());

                // Add the item quad
                Optional<RenderType> renderType = FusionClient.getChunkLayer(quad.renderType()).map(RenderTypeHelper::getEntityRenderType);
                if(renderType.isEmpty() && forgeRenderType != null)
                    renderType = Optional.of(forgeRenderType);
                List<BakedQuad> itemQuads = mesh.computeIfAbsent(renderType, k -> new ArrayList<>());
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(quad.hasConnectingTexture()){
                    mutableQuad.set(ConnectingBakedModel.TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    boolean keepQuad = ConnectingTextureLayoutHandler.get(quad.getLayout()).processItemQuad(quadIndex, mutableQuad, (ConnectingTextureSprite)quad.bakedQuad().sprite());
                    mutableQuad.resetPermutation();
                    if(!keepQuad)
                        continue;
                }
                itemQuads.add(mutableQuad.toBakedQuad());
            }
        }
        this.mesh = mesh.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();

        // Check whether the quads contain animated textures
        boolean animated = false;
        for(BaseModelQuad quad : quads){
            if(quad.bakedQuad().sprite().isAnimated()){
                animated = true;
                break;
            }
        }
        this.animated = animated;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int i){
        renderState.appendModelIdentityElement(this);
        if(this.animated)
            renderState.setAnimated();
        ItemStackRenderState.FoilType foilType = stack.hasFoil() ? BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD : null;
        if(foilType != null){
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }
        int tints = this.tints.size();
        int[] tintValues = new int[tints];
        for(int j = 0; j < tints; j++){
            int tint = this.tints.get(j).calculate(stack, level, entity);
            tintValues[j] = tint;
            renderState.appendModelIdentityElement(tint);
        }
        for(Pair<Optional<RenderType>,List<BakedQuad>> pair : this.mesh){
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(foilType != null)
                layer.setFoilType(foilType);
            System.arraycopy(tintValues, 0, layer.prepareTintLayers(tints), 0, tintValues.length);
            layer.setExtents(this.extents);
            this.properties.applyToLayer(layer, displayContext);
            layer.prepareQuadList().addAll(pair.right());
            layer.setRenderType(pair.left().orElseGet(() -> ItemBlockRenderTypes.getRenderType(stack)));
        }
    }
}
