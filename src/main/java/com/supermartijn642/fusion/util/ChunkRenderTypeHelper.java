package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.Set;

/**
 * Created 24/05/2026 by SuperMartijn642
 */
public class ChunkRenderTypeHelper {

    private static final Set<RenderType> CHUNK_RENDER_TYPE_SET = ImmutableSet.copyOf(allChunkRenderTypes());

    public static List<RenderType> allChunkRenderTypes(){
        return RenderType.chunkBufferLayers();
    }

    public static boolean isChunkRenderType(RenderType chunkRenderType){
        return CHUNK_RENDER_TYPE_SET.contains(chunkRenderType);
    }
}
