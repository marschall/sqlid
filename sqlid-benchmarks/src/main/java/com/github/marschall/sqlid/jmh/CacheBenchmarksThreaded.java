package com.github.marschall.sqlid.jmh;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.util.ConcurrentLruCache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.marschall.sqlid.Cache;
import com.github.marschall.sqlid.HashLruCache;
import com.github.marschall.sqlid.SqlId;

/**
 * Multi-threaded micro-benchmarks for LRU caches with a realistic load function.
 */
@BenchmarkMode(Throughput)
@OutputTimeUnit(MILLISECONDS)
@State(Scope.Benchmark)
@Threads(8)
public class CacheBenchmarksThreaded {

  private static final int CAPACITY = 256;
  
  private static final String[] QUERIES;
  
  static {
    QUERIES = new String[CAPACITY + 1];
    for (int i = 0; i < QUERIES.length; i++) {
      QUERIES[i] = "SELECT * from dual where dummy = " + i;
    }
  }

  /*
   * https://github.com/spring-projects/spring-framework/issues/26320
   */
  private ConcurrentLruCache<String, String> springCache;

  private Cache<String, String> projectCache;

  private com.github.benmanes.caffeine.cache.Cache<String, String> caffeineCache;

  @Setup
  public void doSetup() {
    this.springCache = new ConcurrentLruCache<>(CAPACITY, SqlId::compute);
    this.projectCache = new HashLruCache<>(CAPACITY);
    this.caffeineCache = Caffeine.newBuilder()
            .initialCapacity(CAPACITY)
            .maximumSize(CAPACITY)
            .build();;

    // preload the caches
    for (int i = 0; i < CAPACITY; i++) {
      String query = QUERIES[i];
      this.springCache.get(query);
      this.projectCache.get(query, SqlId::compute);
      this.caffeineCache.put(query, SqlId.compute(query));
    }
  }

  @Benchmark
  public void getInCapacitySpring(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        String query = QUERIES[j];
        blackhole.consume(this.springCache.get(query));
      }
    }
  }

  @Benchmark
  public void getInCapacityCaffeine(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        String query = QUERIES[j];
        blackhole.consume(this.caffeineCache.get(query, SqlId::compute));
      }
    }
  }

  @Benchmark
  public void getInCapacityProject(Blackhole blackhole) {
    for (int i = 0; i < CAPACITY; i++) {
      for (int j = 0; j < i; j++) {
        String query = QUERIES[j];
        blackhole.consume(this.projectCache.get(query, SqlId::compute));
      }
    }
  }

  @Benchmark
  public void getOutOfCapacitySpring(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      String query = QUERIES[i];
      blackhole.consume(this.springCache.get(query));
    }
  }

  @Benchmark
  public void gettOutOfCapacityCaffeine(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      String query = QUERIES[i];
      blackhole.consume(this.caffeineCache.get(query, SqlId::compute));
    }
  }

  @Benchmark
  public void gettOutOfCapacityProject(Blackhole blackhole) {
    for (int i = 0; i < (CAPACITY + 1); i++) {
      String query = QUERIES[i];
      blackhole.consume(this.projectCache.get(query, SqlId::compute));
    }
  }

}
