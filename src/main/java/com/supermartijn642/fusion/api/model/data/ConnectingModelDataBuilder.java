package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelDataBuilderImpl;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public interface ConnectingModelDataBuilder extends VanillaModelDataBuilder<ConnectingModelDataBuilder,ConnectingModelData> {

    static ConnectingModelDataBuilder builder(){
        return new ConnectingModelDataBuilderImpl();
    }

    /**
     * Adds a new connection predicate. Of the added predicates, only one needs to be satisfied to form a connection.
     * In case multiple predicates should be satisfied, use {@link DefaultConnectionPredicates#and(ConnectionPredicate...)}.
     */
    ConnectingModelDataBuilder connection(ConnectionPredicate predicate);

    /**
     * Adds a new connection predicate for the given texture. Of the added predicates, only one needs to be satisfied to form a connection.
     * In case multiple predicates should be satisfied, use {@link DefaultConnectionPredicates#and(ConnectionPredicate...)}.
     */
    ConnectingModelDataBuilder connection(String texture, ConnectionPredicate predicate);
}
