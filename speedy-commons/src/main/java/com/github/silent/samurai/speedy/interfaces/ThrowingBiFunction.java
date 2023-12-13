package com.github.silent.samurai.speedy.interfaces;

// generate documentation using javadoc

/**
 * A functional interface that can be used to throw exceptions from lambda expressions.
 * This is required because lambda expressions cannot throw exceptions.
 * <p>
 * This interface is not intended to be used directly. Instead, use the {@link ThrowingBiFunction} interface.
 *
 * @param <T1> the type of the first input to the function
 * @param <T2> the type of the second input to the function
 * @param <R>  the type of the result of the function
 * @param <E>  the type of exception that the function throws
 * @author Sourav Das
 * @see ThrowingBiFunction
 */
@FunctionalInterface
public interface ThrowingBiFunction<T1, T2, R, E extends Exception> {
    R apply(T1 t1, T2 t2) throws E;
}

