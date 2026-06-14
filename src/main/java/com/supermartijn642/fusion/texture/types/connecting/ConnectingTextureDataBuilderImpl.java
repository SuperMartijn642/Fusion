package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingTextureDataBuilderImpl implements ConnectingTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private ConnectingTextureData.Layout layout = ConnectingTextureData.Layout.FULL;
    private ConnectionPredicate connectionPredicate;
    private RawTextureInstance<?,?> subTexture;
    private boolean perTileAnimation = false;

    @Override
    public ConnectingTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public ConnectingTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public ConnectingTextureDataBuilderImpl tinting(@Nullable BaseTextureData.QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder layout(ConnectingTextureData.Layout layout){
        this.layout = layout;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder connectionPredicate(@Nullable ConnectionPredicate predicate){
        this.connectionPredicate = predicate;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder subTexture(@Nullable RawTextureInstance<?,?> subTextureType){
        this.subTexture = subTextureType;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder perTileAnimation(boolean perTileAnimation){
        this.perTileAnimation = perTileAnimation;
        return this;
    }

    @Override
    public ConnectingTextureData build(){
        return new ConnectingTextureDataImpl(this.renderType, this.emissive, this.tinting, this.layout, this.connectionPredicate, this.subTexture, this.perTileAnimation);
    }
}
