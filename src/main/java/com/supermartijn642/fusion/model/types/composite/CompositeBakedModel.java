package com.supermartijn642.fusion.model.types.composite;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeBakedModel implements IBakedModel, CustomRenderTypeBakedModel {

    private final IBakedModel defaultModel;
    private final List<ConditionalList> entries;
    private final int initialCapacityGuess;

    public CompositeBakedModel(IBakedModel defaultModel, List<ConditionalList> entries){
        this.defaultModel = defaultModel;
        this.entries = entries;

        // Resolve default values
        int guaranteedModelCount = 0;
        for(ConditionalList list : entries){
            for(ModelEntry entry : list.entries){
                if(entry.predicate == null || entry.predicate.alwaysTrue())
                    guaranteedModelCount++;
            }
        }
        this.initialCapacityGuess = Math.min(guaranteedModelCount * 2, entries.size());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        // Check whether we are rendering an item
        ItemStack stack = FusionClient.ITEM_STACK_RENDER_CONTEXT.get();

        // Get block render context
        BlockRenderContext blockRenderContext = FusionClient.BLOCK_RENDER_CONTEXT.get();
        IBlockAccess level = blockRenderContext == null ? null : blockRenderContext.level();
        BlockPos pos = blockRenderContext == null ? null : blockRenderContext.pos();

        // Check whether we need to check the models' render types against the given one
        BlockRenderLayer renderType = MinecraftForgeClient.getRenderLayer();
        boolean shouldCheckRenderType = state != null && renderType != null;
        boolean isDefaultRenderType = !shouldCheckRenderType || ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);

        List<BakedQuad> quads = new ArrayList<>();
        for(ConditionalList list : this.entries){
            IBakedModel model = stack == null ?
                list.get(level, pos, state) :
                list.get(stack);
            if(!shouldCheckRenderType || ModelRenderTypeHelper.canRenderInLayer(model, state, renderType, isDefaultRenderType))
                quads.addAll(model.getQuads(state, cullDirection, seed));
        }
        return quads;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer renderType){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, renderType);
        for(ConditionalList list : this.entries){
            for(ModelEntry entry : list.entries){
                if(ModelRenderTypeHelper.canRenderInLayer(entry.model, state, renderType, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture(){
        return this.defaultModel.getParticleTexture();
    }

    @Override
    public boolean isAmbientOcclusion(){
        return this.defaultModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d(){
        return this.defaultModel.isGui3d();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms(){
        return this.defaultModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides(){
        return this.defaultModel.getOverrides();
    }

    @Override
    public boolean isBuiltInRenderer(){
        return false;
    }

    public static class ConditionalList {
        private final List<ModelEntry> entries;

        public ConditionalList(List<ModelEntry> entries){
            this.entries = entries;
        }

        @Nullable
        IBakedModel get(IBlockAccess level, BlockPos pos, IBlockState state){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForBlockState(level, pos, state))
                    return entry.model;
            }
            return null;
        }

        @Nullable
        IBakedModel get(ItemStack stack){
            for(ModelEntry entry : this.entries){
                if(entry.predicate == null || entry.predicate.testForItem(stack))
                    return entry.model;
            }
            return null;
        }
    }

    public static class ModelEntry {
        private final IBakedModel model;
        private final ModelPredicate predicate;

        public ModelEntry(IBakedModel model, ModelPredicate predicate){
            this.model = model;
            this.predicate = predicate;
        }
    }

    private static class RenderData {
        private final List<IBakedModel> models;

        private RenderData(List<IBakedModel> models){
            this.models = models;
        }
    }
}
