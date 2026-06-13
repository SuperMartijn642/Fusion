package com.supermartijn642.fusion.texture.types.random;

import com.supermartijn642.fusion.api.texture.custom.TextureInstance;

import java.util.List;

/**
 * Created 13/06/2026 by SuperMartijn642
 */
public class StitchedRandomTextureData extends RandomTextureDataImpl {

    private final List<TextureInstance<?>> subTextures;

    public StitchedRandomTextureData(RenderType renderType, boolean emissive, QuadTinting tinting, int rows, int columns, RandomnessSource randomSource, Long seed, List<TextureInstance<?>> subTextures){
        super(renderType, emissive, tinting, rows, columns, randomSource, seed, null, false);
        this.subTextures = subTextures;
    }

    public List<TextureInstance<?>> getSubTextures(){
        return this.subTextures;
    }
}
