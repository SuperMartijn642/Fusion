package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public interface ConnectingModelData extends BaseModelData {

    static ConnectingModelDataBuilder builder(){
        return ConnectingModelDataBuilder.builder();
    }

    ConnectionPredicate getConnectionPredicate(String texture);

    ConnectionPredicate getDefaultConnectionPredicate();

    /**
     * Gets all connection predicates by key.
     * Each key points to either a connection predicate or to another connections key.
     */
    Map<String,Either<ConnectionPredicate,String>> getAllConnectionPredicates();
}
