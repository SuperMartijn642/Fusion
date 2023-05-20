package com.supermartijn642.fusion.api.util;

import com.google.common.base.Objects;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created 23/07/2022 by SuperMartijn642
 */
public class Pair<X, Y> {

    public static <X, Y> Pair<X,Y> of(X left, Y right){
        return new Pair<>(left, right);
    }

    private final X left;
    private final Y right;

    private Pair(X left, Y right){
        this.left = left;
        this.right = right;
    }

    public X left(){
        return this.left;
    }

    public Y right(){
        return this.right;
    }

    /**
     * Applies the given mapper to the left value.
     */
    public <S> Pair<S,Y> mapLeft(Function<X,S> mapper){
        return Pair.of(mapper.apply(this.left), this.right);
    }

    /**
     * Applies the given mapper to the right value.
     */
    public <S> Pair<X,S> mapRight(Function<Y,S> mapper){
        return Pair.of(this.left, mapper.apply(this.right));
    }

    /**
     * Applies the respective mapper to the left or right values.
     */
    public <R, S> Pair<R,S> map(Function<X,R> mapLeft, Function<Y,S> mapRight){
        return Pair.of(mapLeft.apply(this.left), mapRight.apply(this.right));
    }

    /**
     * Maps the values held by this pair to an object of type {@link S}.
     */
    public <S> S flatMap(BiFunction<X,Y,S> mapper){
        return mapper.apply(this.left, this.right);
    }

    /**
     * Applies the given consumer to the values held by this pair.
     */
    public void apply(BiConsumer<X,Y> consumer){
        consumer.accept(this.left, this.right);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || this.getClass() != o.getClass()) return false;
        Pair<?,?> pair = (Pair<?,?>)o;
        return Objects.equal(this.left, pair.left) && Objects.equal(this.right, pair.right);
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.left, this.right);
    }
}
