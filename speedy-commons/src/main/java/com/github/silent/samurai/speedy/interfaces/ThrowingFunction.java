package com.github.silent.samurai.speedy.interfaces;


/**
 * A functional interface that can be used to throw exceptions from lambda expressions.
 * This is required because lambda expressions cannot throw exceptions.
 * <p>
 * This interface is not intended to be used directly. Instead, use the {@link ThrowingBiFunction} interface.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception that the function throws
 * @author Sourav Das
 * @see ThrowingBiFunction
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

