package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2022 by SuperMartijn642
 */
public class TextureAtlases {

    @SuppressWarnings("deprecation")
    private static final ResourceLocation BLOCKS = AtlasTexture.LOCATION_BLOCKS;

    public static ResourceLocation getBlocks(){
        return BLOCKS;
    }
}
