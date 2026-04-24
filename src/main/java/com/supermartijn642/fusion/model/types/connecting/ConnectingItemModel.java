package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class ConnectingItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final List<Pair<RenderType,List<BakedQuad>>> mesh;
    private final Supplier<Vector3fc[]> extents;
    private final boolean animated;

    public ConnectingItemModel(List<ItemTintSource> tints, List<ConnectingModelQuad> quads, ModelRenderProperties properties){
        this.tints = tints;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        Map<RenderType,List<BakedQuad>> mesh = new LinkedHashMap<>();
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = quad.hasConnectingTexture() ? ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount() : 0;
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                mutableQuad.fillFromBakedQuad(quad.bakedQuad());
                mutableQuad.emissive(quad.emissive());

                // Add the item quad
                RenderType renderType = quad.bakedQuad().sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ?
                    Sheets.translucentBlockItemSheet() : Sheets.translucentItemSheet();
                List<BakedQuad> itemQuads = mesh.computeIfAbsent(renderType, k -> new ArrayList<>());
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(quad.hasConnectingTexture()){
                    mutableQuad.set(ConnectingBakedModel.TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    boolean keepQuad = ConnectingTextureLayoutHandler.get(quad.getLayout()).processItemQuad(quadIndex, mutableQuad, quad.spriteInstance(), (StitchedConnectingTextureData)quad.spriteInstance().getTexture().getCustomData());
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
            //noinspection resource
            if(quad.bakedQuad().sprite().contents().isAnimated()){
                animated = true;
                break;
            }
        }
        this.animated = animated;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int i){
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
            int tint = this.tints.get(j).calculate(stack, level, owner == null ? null : owner.asLivingEntity());
            tintValues[j] = tint;
            renderState.appendModelIdentityElement(tint);
        }
        for(Pair<RenderType,List<BakedQuad>> pair : this.mesh){
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(foilType != null)
                layer.setFoilType(foilType);
            System.arraycopy(tintValues, 0, layer.prepareTintLayers(tints), 0, tintValues.length);
            layer.setExtents(this.extents);
            this.properties.applyToLayer(layer, displayContext);
            layer.prepareQuadList().addAll(pair.right());
            layer.setRenderType(pair.left());
        }
    }
}
