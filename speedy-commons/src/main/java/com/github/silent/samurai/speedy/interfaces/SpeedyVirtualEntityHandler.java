package com.github.silent.samurai.speedy.interfaces;

public interface SpeedyVirtualEntityHandler<T> {

    T create(T entity);

    T update(T entity);

    T delete(T entity);

}
