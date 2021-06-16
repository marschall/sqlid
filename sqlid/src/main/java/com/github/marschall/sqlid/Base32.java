package com.github.marschall.sqlid;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Base32 encoding as done by Oracle for SQL_ID computation.
 */
final class Base32 {

  /**
   * The length of sql_id is 13 chars.
   */
  private static final int SQL_ID_SIZE = 13;

  /**
   * The alphabet used for base32 encoding of sql_id, it seems to be a custom variant.
   */
  private static final byte[] BASE32_ALPHABET = new byte[] {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c',  'd', // e missing
      'f', 'g', 'h', // i missing
      'j', 'k', // l missing
      'm', 'n', // o missing
      'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

  private Base32() {
    throw new AssertionError("not instantiable");
  }

  private static byte toBase32(int i) {
    if ((i < 0) || (i > 32)) {
      throw new IllegalArgumentException();
    }
    return BASE32_ALPHABET[i];
  }

  static String toBase32String(long l) {
    byte[] result = new byte[SQL_ID_SIZE];
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

}
