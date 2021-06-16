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
 * <h2>Implementation Notes</h2>
 * Accessing the cache acquires an exclusive lock that is released during the
 * computation of the value to be cached.
 *
 * @param <K> the type of the lookup keys
 * @param <V> the type of the cached values
 */
public final class HashLruCache<K, V> implements Cache<K, V> {

  private final int capacity;

  private final Map<K, Node<K, V>> values;

  private Node<K, V> mostRecentlyUsed;

  private Node<K, V> leastRecentlyUsed;

  private final Lock lock;

  /**
   * Constructs a {@link HashLruCache}.
   * 
   * @param capacity the desired maximum capacity of this cache,
   *                 must be positive
   * @throws IllegalArgumentException if {@code capacity} is not positive
   */
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
      Node<K, V> node = this.values.get(key);
      int currentSize = this.values.size();
      if (node != null) {
        V value = this.updateLru(node, currentSize);
        this.lock.unlock();
        return value;
      } else {
        // recompute the value outside of the lock
        // recomputing the value is likely expensive, otherwise we would not need a cache
        // allow other reads while recomputing
        this.lock.unlock();
        // the value is not in the cache
        V value = loader.apply(key); // this could be done outside the lock
        Objects.requireNonNull(value, "value");

        // acquire lock again
        this.lock.lock();
        this.addNewValue(key, value);
        this.lock.unlock();
        return value;
      }
  }

  private V updateLru(Node<K, V> node, int currentSize) {
    // the value is in the cache
    if ((currentSize > 1) && (node != this.mostRecentlyUsed)) {
      // only update if there is more than 1 item in the cache
      // and the node isn't already the most recently used one
      // per definition the previous node is set
      node.previous.next = node.next;
      if (node.next != null) {
        node.next.previous = node.previous;
      }
      if (node == this.leastRecentlyUsed) {
        this.leastRecentlyUsed = this.leastRecentlyUsed.previous;
      }
      node.previous = null;
      node.next = this.mostRecentlyUsed;
      node.next.previous = node;
      this.mostRecentlyUsed = node;
    }
    return node.value;
  }

  private void addNewValue(K key, V value) {
    // because we computed the value outside the lock the value may now be in the cache
    Node<K, V> readBack = this.values.get(key);
    int currentSize = this.values.size();
    if (readBack != null) {
      this.updateLru(readBack, currentSize);
      // FIXME identity
      return;
    }

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
