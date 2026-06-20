package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.util.FallbackPropertyStore;
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
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
        if(stack.getItem() instanceof BlockItem && !ItemBlockRenderTypes.getRenderLayers(((BlockItem)stack.getItem()).getBlock().defaultBlockState()).contains(RenderType.translucent()))
            defaultRenderType = Sheets.cutoutBlockSheet();
        else
            defaultRenderType = Sheets.translucentItemSheet();
        // Submit each part
        PropertyStore propertyStore = FallbackPropertyStore.create(this.propertyStore);

        // Collect quads by render type
        List<RenderType> renderTypes = new ArrayList<>(4);
        List<List<BakedQuad>> quadsByRenderType = new ArrayList<>(4);
        Consumer<QuadAccess> submitter = quad -> {
            // Get render type
            RenderType renderType = quad.itemRenderType();
            if(renderType == null)
                renderType = defaultRenderType;
            // Get or quad list
            int i = renderTypes.indexOf(renderType);
            List<BakedQuad> bakedQuads;
            if(i == -1){
                renderTypes.add(renderType);
                bakedQuads = new ArrayList<>();
                quadsByRenderType.add(bakedQuads);
            }else
                bakedQuads = quadsByRenderType.get(i);
            // Add the quad to the list
            bakedQuads.add(quad.toBakedQuad());
        };

        // Process all quads
        EmittableQuad mutableQuad = null;
        for(Quad quad : this.quads){
            // Simply add quads that don't need further processing
            if(quad.processor() == null){
                submitter.accept(quad.quad());
                continue;
            }

            // Create mutable quad
            if(mutableQuad == null)
                mutableQuad = EmittableQuad.create(submitter::accept);
            mutableQuad.copyFrom(quad.quad());

            // Process quad
            Object state = quad.processor().extractState(stack, propertyStore);
            quad.processor().processQuad(mutableQuad, quad.sprite(), state, propertyStore);
        }

        // Create layer for each render type
        for(int i = 0; i < renderTypes.size(); i++){
            RenderType renderType = renderTypes.get(i);
            List<BakedQuad> bakedQuads = quadsByRenderType.get(i);
            // Create layer
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(tintValues != null)
                System.arraycopy(tintValues, 0, layer.prepareTintLayers(tintValues.length), 0, tintValues.length);
            if(foilType != null)
                layer.setFoilType(foilType);
            layer.setupBlockModel(
                new SimpleBakedModel(
                    bakedQuads,
                    UnknownModelType.EMPTY_CULLED_QUADS,
                    true,
                    this.guiLight.lightLikeBlock(),
                    true,
                    this.particleSprite,
                    this.transforms
                ),
                renderType
            );
        }
    }

    public record Quad(QuadAccess quad, SpriteInstance sprite, ItemQuadProcessor<Object> processor) {
    }
}
