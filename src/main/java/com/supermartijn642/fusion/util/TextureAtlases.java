package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2022 by SuperMartijn642
 */
public class TextureAtlases {

    private static final ResourceLocation BLOCKS = TextureMap.LOCATION_BLOCKS_TEXTURE;

    public static ResourceLocation getBlocks(){
        return BLOCKS;
    }
}
