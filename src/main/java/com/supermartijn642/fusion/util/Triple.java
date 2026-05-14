package com.supermartijn642.fusion.util;

/**
 * Created 23/07/2022 by SuperMartijn642
 */
public final class Triple<X, Y, Z> {

    public static <X, Y, Z> Triple<X,Y,Z> of(X left, Y middle, Z right){
        return new Triple<>(left, middle, right);
    }

    private final X left;
    private final Y middle;
    private final Z right;

    public Triple(X left, Y middle, Z right){
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public X left(){
        return this.left;
    }

    public Y middle(){
        return this.middle;
    }

    public Z right(){
        return this.right;
    }
}
