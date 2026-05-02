package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class BaseTextureDataImpl implements BaseTextureData {

    private final boolean emissive;
    private final QuadTinting tinting;

    public BaseTextureDataImpl(boolean emissive, QuadTinting tinting){
        this.emissive = emissive;
        this.tinting = tinting;
    }

    @Override
    public boolean isEmissive(){
        return this.emissive;
    }

    @Override
    public QuadTinting getTinting(){
        return this.tinting;
    }
}
