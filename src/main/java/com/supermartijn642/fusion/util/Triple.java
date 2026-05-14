package com.supermartijn642.fusion.util;

/**
 * Created 23/07/2022 by SuperMartijn642
 */
public record Triple<X, Y, Z>(X left, Y middle, Z right) {

    public static <X, Y, Z> Triple<X,Y,Z> of(X left, Y middle, Z right){
        return new Triple<>(left, middle, right);
    }
}
