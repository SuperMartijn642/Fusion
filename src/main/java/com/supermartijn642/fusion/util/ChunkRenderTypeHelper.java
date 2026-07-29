package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created 24/05/2026 by SuperMartijn642
 */
public class ChunkRenderTypeHelper {

    private static final List<RenderType> RENDER_TYPES = RenderType.chunkBufferLayers();
    private static final int COUNT;
    private static final Map<RenderType,Integer> TO_ID;
    private static final RenderType[] BY_ID;

    static{
        List<RenderType> renderTypes = ChunkRenderTypeHelper.all();
        COUNT = renderTypes.size() + 1;
        ImmutableMap.Builder<RenderType,Integer> renderTypeToId = ImmutableMap.builderWithExpectedSize(renderTypes.size());
        BY_ID = new RenderType[renderTypes.size() + 1];
        for(int i = 0; i < renderTypes.size(); i++){
            RenderType renderType = renderTypes.get(i);
            renderTypeToId.put(renderType, i + 1);
            BY_ID[i + 1] = renderType;
        }
        TO_ID = renderTypeToId.build();
    }

    public static List<RenderType> all(){
        return RenderType.chunkBufferLayers();
    }

    public static boolean isChunkRenderType(RenderType chunkRenderType){
        return RENDER_TYPES.contains(chunkRenderType);
    }

    public static int getCount(){
        return COUNT;
    }

    public static int getId(@Nullable RenderType renderType){
        if(renderType == null)
            return 0;
        Integer id = TO_ID.get(renderType);
        if(id == null)
            throw new IllegalArgumentException("Key must be a chunk render type!");
        return id;
    }

    @Nullable
    public static RenderType byId(int id){
        return BY_ID[id];
    }
}
