package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class MD5HashTests {

  static Stream<String> input() {
    return IntStream.range(0, 128)
                     .mapToObj(MD5HashTests::generateInput);
  }
  
  @ParameterizedTest
  @MethodSource("input")
  void md5Equals(String s) {
    assertEquals(MD5.nonAsciiMd5Hash(s), MD5.asciiMd5Hash(s));
  }

  @ParameterizedTest
  @MethodSource("input")
  void hashEquals(String s) {
    assertEquals(OriginalSqlId.SQL_ID(s), SqlId.compute(s));
  }

  private static StringBuilder generateBuffer(int length) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < length; i++) {
      buffer.append((char) i);
    }
    return buffer;
  }

  private static String generateInput(int length) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < length; i++) {
      buffer.append((char) i);
    }
    // TODO avoid duplication
    return buffer.toString();
  }

}
