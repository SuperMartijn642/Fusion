package com.supermartijn642.fusion.api.util;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 09/09/2022 by SuperMartijn642
 */
public abstract class Either<X, Y> {

    /**
     * Creates a left either instance with the given value.
     */
    public static <X, Y> Either<X,Y> left(X object){
        return new Left<>(object);
    }

    /**
     * Creates a right either instance with the given value.
     */
    public static <X, Y> Either<X,Y> right(Y object){
        return new Right<>(object);
    }

    private Either(){
    }

    /**
     * Whether the either is a left value or not.
     */
    public abstract boolean isLeft();

    /**
     * Whether the either is a right value or not.
     */
    public abstract boolean isRight();

    /**
     * Will take this either as a left value.
     * @throws NoSuchElementException if the either is a right value
     */
    public abstract X left();

    /**
     * Will take this either as a right value.
     * @throws NoSuchElementException if the either is a left value
     */
    public abstract Y right();

    /**
     * Will take this either as a left value. If this either is a right value, the given alternative will be returned.
     */
    public abstract X leftOrElse(X other);

    /**
     * Will take this either as a right value. If this either is a left value, the given alternative will be returned.
     */
    public abstract Y rightOrElse(Y other);

    /**
     * Will take this either as a left value. If this either is a right value, the given alternative will be resolved and returned.
     */
    public abstract X leftOrElseGet(Supplier<X> other);

    /**
     * Will take this either as a right value. If this either is a left value, the given alternative will be resolved and returned.
     */
    public abstract Y rightOrElseGet(Supplier<Y> other);

    /**
     * Will take this either as a left value. If this either is a right value, {@code null} will be returned.
     */
    public X leftOrNull(){
        return this.leftOrElse(null);
    }

    /**
     * Will take this either as a right value. If this either is a left value, {@code null} will be returned.
     */
    public Y rightOrNull(){
        return this.rightOrElse(null);
    }

    /**
     * Applies the given mapper if this either is a left value.
     */
    public abstract <S> Either<S,Y> mapLeft(Function<X,S> mapper);

    /**
     * Applies the given mapper if this either is a right value.
     */
    public abstract <S> Either<X,S> mapRight(Function<Y,S> mapper);

    /**
     * Applies the respective mapper given whether this either is a left or right value.
     */
    public <R, S> Either<R,S> map(Function<X,R> mapLeft, Function<Y,S> mapRight){
        return this.mapLeft(mapLeft).mapRight(mapRight);
    }

    /**
     * Applies the respective mapper to obtain an object of type {@link S}.
     */
    public abstract <S> S flatMap(Function<X,S> mapLeft, Function<Y,S> mapRight);

    /**
     * Applies the given consumer if this either is a left value.
     */
    public abstract void ifLeft(Consumer<X> consumer);

    /**
     * Applies the given consumer if this either is a right value.
     */
    public abstract void ifRight(Consumer<Y> consumer);

    private static class Left<X, Y> extends Either<X,Y> {

        private final X value;

        private Left(X value){
            this.value = value;
        }

        @Override
        public boolean isLeft(){
            return true;
        }

        @Override
        public boolean isRight(){
            return false;
        }

        @Override
        public X left(){
            return this.value;
        }

        @Override
        public Y right(){
            throw new NoSuchElementException("Right value is not present!");
        }

        @Override
        public X leftOrElse(X other){
            return this.value;
        }

        @Override
        public Y rightOrElse(Y other){
            return other;
        }

        @Override
        public X leftOrElseGet(Supplier<X> other){
            return this.value;
        }

        @Override
        public Y rightOrElseGet(Supplier<Y> other){
            return other.get();
        }

        @Override
        public <S> Either<S,Y> mapLeft(Function<X,S> mapper){
            return new Left<>(mapper.apply(this.value));
        }

        @Override
        public <S> Either<X,S> mapRight(Function<Y,S> mapper){
            //noinspection unchecked
            return (Either<X,S>)this;
        }

        @Override
        public <S> S flatMap(Function<X,S> mapLeft, Function<Y,S> mapRight){
            return mapLeft.apply(this.value);
        }

        @Override
        public void ifLeft(Consumer<X> consumer){
            consumer.accept(this.value);
        }

        @Override
        public void ifRight(Consumer<Y> consumer){
        }
    }

    private static class Right<X, Y> extends Either<X,Y> {

        private final Y value;

        private Right(Y value){
            this.value = value;
        }

        @Override
        public boolean isLeft(){
            return false;
        }

        @Override
        public boolean isRight(){
            return true;
        }

        @Override
        public X left(){
            throw new NoSuchElementException("Left value is not present!");
        }

        @Override
        public Y right(){
            return this.value;
        }

        @Override
        public X leftOrElse(X other){
            return other;
        }

        @Override
        public Y rightOrElse(Y other){
            return this.value;
        }

        @Override
        public X leftOrElseGet(Supplier<X> other){
            return other.get();
        }

        @Override
        public Y rightOrElseGet(Supplier<Y> other){
            return this.value;
        }

        @Override
        public <S> Either<S,Y> mapLeft(Function<X,S> mapper){
            //noinspection unchecked
            return (Either<S,Y>)this;
        }

        @Override
        public <S> Either<X,S> mapRight(Function<Y,S> mapper){
            return new Right<>(mapper.apply(this.value));
        }

        @Override
        public <S> S flatMap(Function<X,S> mapLeft, Function<Y,S> mapRight){
            return mapRight.apply(this.value);
        }

        @Override
        public void ifLeft(Consumer<X> consumer){
        }

        @Override
        public void ifRight(Consumer<Y> consumer){
            consumer.accept(this.value);
        }
    }
}
