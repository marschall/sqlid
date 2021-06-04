package com.github.marschall.sqlid;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SqlId {

  /**
   * Max sql_id length is 13 chars.
   */
  private static final int RESULT_SIZE = 13;

  private static final byte[] ALPHABET = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

  private SqlId() {
    throw new AssertionError("not instantiable");
  }

  /**
   * Compute sqlid for a statement, the same way as Oracle does
   * http://www.slaviks-blog.com/2010/03/30/oracle-sql_id-and-hash-value/
   * https://blog.tanelpoder.com/2009/02/22/sql_id-is-just-a-fancy-representation-of-hash-value/
   *
   * @param stmt SQL string without trailing 0x00 Byte
   * @return sql_id as computed by Oracle
   */
  static String SQL_ID(String stmt) {
    // compute MD5 sum from SQL string - including trailing 0x00 Byte
    byte[] message = stmt.getBytes(StandardCharsets.UTF_8);
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 not supported", e);
    }
    md.update(message);
    // append a trailing 0x00 byte
    md.update((byte) 0x00);
    byte[] b = md.digest();

    // bytes 0 - 7 from the hash are not used, only the last 64bits are used
    // therefore we can use a 64bit long

    // most significant unsigned long
    long sqln = mostSignificantLong(b);

    return toBase32String(sqln);
  }

  private static long mostSignificantLong(byte[] b) {
    if (b.length != 16) {
      throw new IllegalArgumentException();
    }
    return ((b[11] & 0xFFl) << 56)
            | ((b[10] & 0xFFl) << 48)
            | ((b[9] & 0xFFl) << 40)
            | ((b[8] & 0xFFl) << 32)
            | ((b[15] & 0xFFl) << 24)
            | ((b[14] & 0xFFl) << 16)
            | ((b[13] & 0xFFl) << 8)
            | (b[12] & 0xFFl);
  }

  private static String toBase32String(long l) {
    // Compute Base32, take 13x 5bits
    // max sql_id length is 13 chars, 13 x 5 => 65bits most significant is always 0
    byte[] result = new byte[RESULT_SIZE];
    result[0] = toBase32((int) ((l & (0b11111L << 60)) >>> 60));
    result[1] = toBase32((int) ((l & (0b11111L << 55)) >>> 55));
    result[2] = toBase32((int) ((l & (0b11111L << 50)) >>> 50));
    result[3] = toBase32((int) ((l & (0b11111L << 45)) >>> 45));
    result[4] = toBase32((int) ((l & (0b11111L << 40)) >>> 40));
    result[5] = toBase32((int) ((l & (0b11111L << 35)) >>> 35));
    result[6] = toBase32((int) ((l & (0b11111L << 30)) >>> 30));
    result[7] = toBase32((int) ((l & (0b11111L << 25)) >>> 25));
    result[8] = toBase32((int) ((l & (0b11111L << 20)) >>> 20));
    result[9] = toBase32((int) ((l & (0b11111L << 15)) >>> 15));
    result[10] = toBase32((int) ((l & (0b11111L << 10)) >>> 10));
    result[11] = toBase32((int) ((l & (0b11111L << 5)) >>> 5));
    result[12] = toBase32((int) (l & 0b11111L));
    return new String(result, ISO_8859_1); // US_ASCII fast path is only in JDK 17+
  }

  private static byte toBase32(int i) {
    if ((i < 0) || (i > 32)) {
      throw new IllegalArgumentException();
    }
    return ALPHABET[i];
//    if (i < 10) {
//      return (byte) ('0' + i);
//    } else {
//      return (byte) ('a' + (i - 9));
//    }
  }

  public static void main(String[] args) {
    System.out.println(0x100);
    System.out.println(ALPHABET.length);
    System.out.println(Integer.toHexString(31));
    System.out.println(Integer.toBinaryString(31));
    System.out.println(1 << 8);
  }

}
