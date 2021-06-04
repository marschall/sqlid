package com.github.marschall.sqlid;

import java.util.function.Function;

public interface Cache<K, V> {

  V get(K key, Function<K, V> loader);

}
