package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.predicate.ConnectionDirection;

/**
 * Created 10/05/2023 by SuperMartijn642
 */
public enum TextureRotation {

    _0(0), _90(1), _180(2), _270(3);

    private final int offset;

    TextureRotation(int offset){
        this.offset = offset * 2;
    }

    public ConnectionDirection rotate(ConnectionDirection direction){
        return ConnectionDirection.values()[(direction.ordinal() + this.offset) % 8];
    }
}
