package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.AbstractBaseModelDataBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl extends AbstractBaseModelDataBuilder<ConnectingModelDataBuilderImpl,ConnectingModelData> implements ConnectingModelData.ConnectingModelDataBuilder<ConnectingModelDataBuilderImpl,ConnectingModelData> {

    public static ConnectingModelData.ConnectingModelDataBuilder<?,ConnectingModelData> builder(){
        return new ConnectingModelDataBuilderImpl();
    }

    private final Map<String,Either<String,ConnectionPredicate>> connections = new HashMap<>();

    private ConnectingModelDataBuilderImpl(){
    }

    @Override
    public ConnectingModelDataBuilderImpl connections(String key, Either<String,ConnectionPredicate> predicate){
        if(!key.matches("[a-zA-Z0-9_]*"))
            throw new IllegalArgumentException("Connections key must only contain characters [a-zA-Z0-9_]!");
        if(this.connections.containsKey(key))
            throw new RuntimeException("Duplicate connections entry for key '" + key + "'!");

        // Remove '#' character from references
        if(predicate.isLeft() && !predicate.left().isEmpty() && predicate.left().charAt(0) == '#')
            this.connections.put(key, Either.left(predicate.left().substring(1)));
        else
            this.connections.put(key, predicate);
        return this;
    }

    @Override
    public ConnectingModelData build(){
        return new ConnectingModelDataImpl(
            this.parent,
            this.materials,
            this.ambientOcclusion,
            this.shade,
            this.emissive,
            this.isGui3d,
            this.elements == null ? null : CuboidModelGeometry.of(this.elements),
            this.itemTransforms,
            this.itemOverrides,
            this.connections
        );
    }
}
