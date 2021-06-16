package com.github.marschall.sqlid;

import java.util.function.Function;

/**
 * A simple abstraction for allows plugin in different implementations.
 *
 * @param <K> the type of the lookup keys
 * @param <V> the type of the cached values
 */
public interface Cache<K, V> {

  /**
   * Looks up a value in the cache. If none is found computes a new value
   * and stores it, possibly ejecting an existing one.
   * 
   * @param key the lookup key, not {@code null}
   * @param loader the function to compute the value based on the lookup up
   *               should it not already be in the cache,
   *               not {@code null},
   *               must not return {@code null}
   * @return the value, if it wasn't in the cache before the call it was
   *         computed using {@code loader} and now is in the cache
   */
  V get(K key, Function<? super K, ? extends V> loader);

}
