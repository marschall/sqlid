package com.github.marschall.sqlid.jmh;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import com.github.marschall.sqlid.SqlId;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MILLISECONDS)
public class SqlIdBenchmarks {

  @Benchmark
  public String original() {
    return OriginalSqlId.SQL_ID("SELECT * from dual where dummy = :1 ");
  }

  @Benchmark
  public String project() {
    return SqlId.SQL_ID("SELECT * from dual where dummy = :1 ");
  }

}
