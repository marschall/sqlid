package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class MD5HashTests {

  static List<String> input() {
    int inputSize = 128;
    StringBuilder buffer = new StringBuilder(inputSize);
    List<String> input = new ArrayList<>(inputSize);
    for (int i = 0; i < inputSize; i++) {
      input.add(buffer.toString());
      buffer.append((char) i);
    }
    input.add(buffer.toString());
    return input;
  }

  @ParameterizedTest
  @MethodSource("input")
  void md5Equals(String s) {
    assertEquals(referenceMd5Hash(s), MD5.getBinarySqlId(s));
  }

  @ParameterizedTest
  @MethodSource("input")
  void hashEquals(String s) {
    assertEquals(OriginalSqlId.SQL_ID(s), SqlId.compute(s));
  }

  private static long referenceMd5Hash(String s) {

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
    byte[] b = messageDigest.digest();

    // bytes 0 to 7 from the MD5 hash are not used, only the last 64bits are used
    // therefore we can use a 64bit long

    // most significant unsigned long
    return MD5.mostSignificantLong(b);
  }

}
