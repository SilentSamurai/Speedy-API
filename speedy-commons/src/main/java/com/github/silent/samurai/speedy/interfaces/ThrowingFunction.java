package com.github.silent.samurai.speedy.interfaces;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

