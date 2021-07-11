package com.github.marschall.sqlid.jmh;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    return SqlId.compute("SELECT * from dual where dummy = :1 ");
  }

  @Benchmark
  public byte[] jdkHash() {
    return jdkHash("SELECT * from dual where dummy = :1 ");
  }

  private static byte[] jdkHash(String s) {

    // compute the MD5 hash of the SQL
    // it's not clear whether the MD5 hash is computed based on UTF-8 or the database encoding
    byte[] message = s.getBytes(StandardCharsets.UTF_8);
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 not supported", e);
    }
    messageDigest.update(message);
    // append a trailing 0x00 byte
    messageDigest.update((byte) 0x00);
    return messageDigest.digest();
  }

}
