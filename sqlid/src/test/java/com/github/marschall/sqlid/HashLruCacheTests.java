package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashLruCacheTests {

  @Test
  void sizeOne() {
    Cache<Integer, Integer> cache = new HashLruCache<>(1);
    assertEquals(2, cache.get(1, i -> i * 2));
    // 1 is cached, not recomputed
    assertEquals(2, cache.get(1, i -> i * 3));

    assertEquals(4, cache.get(2, i -> i * 2));
    // 2 is cached, not recomputed
    assertEquals(4, cache.get(2, i -> i * 4));

    // 1 is not cached, recomputed
    assertEquals(5, cache.get(1, i -> i * 5));
  }

  @Test
  void sizeTwo() {
    Cache<Integer, Integer> cache = new HashLruCache<>(2);
    assertEquals(2, cache.get(1, i -> i * 2));
    assertEquals(4, cache.get(2, i -> i * 2));
    // 1 is removed, 3 is added
    assertEquals(6, cache.get(3, i -> i * 2));

    // 2 is cached, not recomputed
    assertEquals(4, cache.get(2, i -> i * 3));
    // 3 is cached, not recomputed
    assertEquals(6, cache.get(3, i -> i * 3));

    // 1 is not cached, recomputed, 2 is removed
    assertEquals(5, cache.get(1, i -> i * 5));
    // 2 is not cached, recomputed
    assertEquals(10, cache.get(2, i -> i * 5));
  }

  @Test
  void sizeThree() {
    Cache<Integer, Integer> cache = new HashLruCache<>(3);
    assertEquals(2, cache.get(1, i -> i * 2));
    assertEquals(4, cache.get(2, i -> i * 2));
    assertEquals(6, cache.get(3, i -> i * 2));

    // 2 is cached, not recomputed
    assertEquals(4, cache.get(2, i -> i * 3));
    
    // order is now 2, 3, 1

    // 1 is removed, 4 is added
    assertEquals(8, cache.get(4, i -> i * 2));
    // 3 is removed, 5 is added
    assertEquals(10, cache.get(5, i -> i * 2));

    // 2 is cached, not recomputed
    assertEquals(4, cache.get(2, i -> i * 3));
  }

}
