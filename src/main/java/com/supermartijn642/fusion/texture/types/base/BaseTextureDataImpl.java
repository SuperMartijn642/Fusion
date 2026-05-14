package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class BaseTextureDataImpl implements BaseTextureData {

    private final RenderType renderType;
    private final boolean emissive;
    private final QuadTinting tinting;

    public BaseTextureDataImpl(RenderType renderType, boolean emissive, QuadTinting tinting){
        this.renderType = renderType;
        this.emissive = emissive;
        this.tinting = tinting;
    }

    @Override
    public @Nullable RenderType getRenderType(){
        return this.renderType;
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
