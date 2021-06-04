package com.github.marschall.sqlid;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * A hash map and linked list based implementation of {@link Cache} that uses
 * the Least Recently Used (LRU) algorithm.
 *
 * @param <K>
 * @param <V>
 */
public final class HashLruCache<K, V> implements Cache<K, V> {

  // TODO bench agains org.springframework.util.ConcurrentLruCache<K, V>

  private final int capacity;

  private final Map<K, Node<K, V>> values;

  private Node<K, V> mostRecentlyUsed;

  private Node<K, V> leastRecentlyUsed;

  private final Lock lock;

  public HashLruCache(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.capacity = capacity;
    this.values = new HashMap<>(capacity);
    this.lock = new ReentrantLock(false);
  }

  @Override
  public V get(K key, Function<K, V> loader) {

    Node<K, V> node = this.values.get(key);
    int currentSize = this.values.size();
    if (node != null) {
      if (currentSize > 1) {
        if (node.previous != null) {
          node.previous.next = node.next;
        }
        if (node.next != null) {
          node.next.previous = node.previous;
        }
        if (node == this.leastRecentlyUsed) {
          this.leastRecentlyUsed = this.leastRecentlyUsed.previous;
        }
        node.previous = null;
        this.mostRecentlyUsed = node;
      }
      return node.value;
    } else {
      V value = loader.apply(key);
      Node<K, V> newNode = new Node<>(key, value);
      if (currentSize == 0) {
        this.leastRecentlyUsed = newNode;
      } else {
        this.mostRecentlyUsed.previous = node;
      }
      newNode.next = this.mostRecentlyUsed;
      this.mostRecentlyUsed = newNode;
      if (currentSize < this.capacity) {

      } else {

      }
      return value;
    }
  }

  static final class Node<NK, NV> {

    final NK key;

    final NV value;

    Node<NK, NV> previous;

    Node<NK, NV> next;

    Node(NK key, NV value) {
      this.key = key;
      this.value = value;
    }

  }

}
