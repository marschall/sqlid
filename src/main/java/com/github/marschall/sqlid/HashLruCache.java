package com.github.marschall.sqlid;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(loader, "loader");
    this.lock.lock();
    try {
      return this.getLocked(key, loader);
    } finally {
      this.lock.unlock();
    }
  }
  private V getLocked(K key, Function<K, V> loader) {

    Node<K, V> node = this.values.get(key);
    int currentSize = this.values.size();
    if (node != null) {
      // the value is in the cache
      if ((currentSize > 1) && (node != this.mostRecentlyUsed)) {
        // only update if there is more than 1 item in the cache
        // and the node isn't already the most recently used one
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
        node.next = this.mostRecentlyUsed;
        if (node.next != null) {
          node.next.previous = node;
        }
        this.mostRecentlyUsed = node;
      }
      return node.value;
    } else {
      // the value is not in the cache
      V value = loader.apply(key); // this could be done outside the lock
      Objects.requireNonNull(value, "value");
      Node<K, V> newNode;
      if (currentSize == this.capacity) {
        // the least recently used node has to be removed
        newNode = this.values.remove(this.leastRecentlyUsed.key);
        newNode.key = key;
        newNode.value = value;
        if (this.capacity > 1) {
          this.leastRecentlyUsed.previous.next = null;
          this.leastRecentlyUsed = this.leastRecentlyUsed.previous;
        }
        newNode.previous = null;
      } else {
        // just add the new node
        newNode = new Node<>(key, value);
      }
      if ((currentSize == 0) || (this.capacity == 1)) {
        this.leastRecentlyUsed = newNode;
      } else {
        this.mostRecentlyUsed.previous = newNode;
      }
      newNode.next = this.mostRecentlyUsed;
      this.mostRecentlyUsed = newNode;
      this.values.put(key, newNode);
      return value;
    }
  }

  static final class Node<NK, NV> {

    NK key;

    NV value;

    Node<NK, NV> previous;

    Node<NK, NV> next;

    Node(NK key, NV value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
        return this.key + "=" + this.value;
    }

  }

}
