package com.supermartijn642.fusion.util;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * Created 30/04/2022 by SuperMartijn642
 */
public class TextureAtlases {

    @SuppressWarnings("deprecation")
    private static final Identifier BLOCKS = TextureAtlas.LOCATION_BLOCKS;

    public static Identifier getBlocks(){
        return BLOCKS;
    }
}
