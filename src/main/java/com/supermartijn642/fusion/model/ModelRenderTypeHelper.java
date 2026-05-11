package com.supermartijn642.fusion.model;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraftforge.registries.IRegistryDelegate;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created 13/01/2025 by SuperMartijn642
 */
public class ModelRenderTypeHelper {

    private static final Field FORGE_BLOCK_RENDER_CHECKS_FIELD;

    static{
        try{
            FORGE_BLOCK_RENDER_CHECKS_FIELD = RenderTypeLookup.class.getDeclaredField("blockRenderChecks");
            FORGE_BLOCK_RENDER_CHECKS_FIELD.setAccessible(true);
        }catch(NoSuchFieldException e){
            throw new RuntimeException("Fusion failed to access Forge's RenderTypeLookup#blockRenderChecks field!", e);
        }
    }

    /**
     * Checks whether the given block state should render in the given layer without Fusion overwriting it.
     */
    public static boolean couldBlockRenderInLayerOriginally(BlockState state, RenderType layer){
        Block block = state.getBlock();
        if(block instanceof LeavesBlock)
            return RenderTypeLookup.renderCutout ? layer == RenderType.cutoutMipped() : layer == RenderType.solid();
        else{
            // Get Forge render type check
            Predicate<RenderType> predicate;
            try{
                //noinspection unchecked
                Map<IRegistryDelegate<Block>,Predicate<RenderType>> checks = (Map<IRegistryDelegate<Block>,Predicate<RenderType>>)FORGE_BLOCK_RENDER_CHECKS_FIELD.get(null);
                synchronized(RenderTypeLookup.class){
                    predicate = checks.get(block.delegate);
                }
            }catch(IllegalAccessException e){
                throw new RuntimeException("Could not access Forge's RenderTypeLookup#blockRenderChecks field!", e);
            }
            return predicate != null ? predicate.test(layer) : layer == RenderType.solid();
        }
    }

    public static boolean canRenderInLayer(IBakedModel model, BlockState state, RenderType layer, boolean defaultValue){
        if(state == null || layer == null)
            return defaultValue;
        return model instanceof CustomRenderTypeBakedModel ?
            ((CustomRenderTypeBakedModel)model).canRenderInLayer(state, layer) :
            defaultValue;
    }
}
