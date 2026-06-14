package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingTextureDataImpl extends BaseTextureDataImpl implements ConnectingTextureData {

    private final Layout layout;
    private final ConnectionPredicate connectionPredicate;
    private final RawTextureInstance<?,?> subTexture;
    private final boolean perTileAnimation;

    public ConnectingTextureDataImpl(BaseTextureData.RenderType renderType, boolean emissive, QuadTinting tinting, Layout layout, ConnectionPredicate connectionPredicate, RawTextureInstance<?,?> subTexture, boolean perTileAnimation){
        super(renderType, emissive, tinting);
        this.layout = layout;
        this.connectionPredicate = connectionPredicate;
        this.subTexture = subTexture;
        this.perTileAnimation = perTileAnimation;
    }

    @Override
    public Layout getLayout(){
        return this.layout;
    }

    @Override
    public @Nullable ConnectionPredicate getConnectionPredicate(){
        return this.connectionPredicate;
    }

    @Override
    public @Nullable RawTextureInstance<?,?> subTexture(){
        return this.subTexture;
    }

    @Override
    public boolean perTileAnimation(){
        return this.perTileAnimation;
    }
}
