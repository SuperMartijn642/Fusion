package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 30/04/2022 by SuperMartijn642
 */
public class TextureAtlases {

    @SuppressWarnings("deprecation")
    private static final ResourceLocation BLOCKS = TextureAtlas.LOCATION_BLOCKS;

    public static ResourceLocation getBlocks(){
        return BLOCKS;
    }
}
