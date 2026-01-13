package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelDataBuilderImpl;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public interface ConnectingModelDataBuilder extends BaseModelDataBuilder<ConnectingModelDataBuilder,ConnectingModelData> {

    static ConnectingModelDataBuilder builder(){
        return new ConnectingModelDataBuilderImpl();
    }

    /**
     * Sets the default connection predicate for this model.
     * @see DefaultConnectionPredicates
     */
    ConnectingModelDataBuilder defaultConnections(ConnectionPredicate predicate);

    /**
     * Sets the connection predicate for the given key.
     * @see DefaultConnectionPredicates
     */
    ConnectingModelDataBuilder connections(String key, ConnectionPredicate predicate);

    /**
     * Sets the connections for key {@code key} to redirect to key {@code reference}.
     * @see DefaultConnectionPredicates
     */
    ConnectingModelDataBuilder connections(String key, String reference);
}
