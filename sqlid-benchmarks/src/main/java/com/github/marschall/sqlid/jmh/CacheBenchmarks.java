package com.github.marschall.sqlid.jmh;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.util.ConcurrentLruCache;

import com.github.marschall.sqlid.Cache;
import com.github.marschall.sqlid.HashLruCache;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MILLISECONDS)
@State(Scope.Benchmark)
public class CacheBenchmarks {

  private static final int CAPACITY = 256;

  /*
   * https://github.com/spring-projects/spring-framework/issues/26320
   */
  private ConcurrentLruCache<Integer, Integer> springCache;

  private Cache<Integer, Integer> projectCache;

  private ConcurrentMap<Integer, Integer> caffeineMap;

  @Setup
  public void doSetup() {
    this.springCache = new ConcurrentLruCache<>(CAPACITY, Function.identity());
    this.projectCache = new HashLruCache<>(CAPACITY);
    this.caffeineMap = new ConcurrentLinkedHashMap.Builder<Integer, Integer>()
            .initialCapacity(CAPACITY)
            .maximumWeightedCapacity(CAPACITY)
            .build();

    // preload the caches
    for (int i = 0; i < CAPACITY; i++) {
      Integer key = i;
      this.springCache.get(key);
      this.projectCache.get(key, Function.identity());
      this.caffeineMap.put(key, key);
    }
  }

  @Benchmark
  public void getInCapacitySpring(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        blackhole.consume(this.springCache.get(i));
      }
    }
  }

  @Benchmark
  public void getInCapacityCaffeine(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        blackhole.consume(this.caffeineMap.get(i));
      }
    }
  }

  @Benchmark
  public void getInCapacityProject(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        blackhole.consume(this.projectCache.get(i, Function.identity()));
      }
    }
  }

  @Benchmark
  public void getOutOfCapacitySpring(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      blackhole.consume(this.springCache.get(i));
    }
  }

  @Benchmark
  public void gettOutOfCapacityCaffeine(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      blackhole.consume(this.caffeineMap.get(i));
    }
  }

  @Benchmark
  public void gettOutOfCapacityProject(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      blackhole.consume(this.projectCache.get(i, Function.identity()));
    }
  }

}
