package com.github.marschall.sqlid;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.Objects;

import oracle.jdbc.OracleDatabaseException;

/**
 * Computes Oracle sql_id of SQL statement.
 *
 * @see <a href="https://tanelpoder.com/2009/02/22/sql_id-is-just-a-fancy-representation-of-hash-value/">SQL_ID is just a fancy representation of hash value</a>
 * @see <a href="https://web.archive.org/web/20170510061149/http://www.slaviks-blog.com/2010/03/30/oracle-sql_id-and-hash-value/">Oracle sql_id and hash value</a>
 */
public final class SqlId {

  /**
   * The sql_id length is 13 chars.
   */
  private static final int SQL_ID_SIZE = 13;

  /**
   * The base32 alphabet used for sql_id, it seems to be a custom variant.
   */
  private static final byte[] BASE32_ALPHABET = new byte[] {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c',  'd', // e missing
      'f', 'g', 'h', // i missing
      'j', 'k', // l missing
      'm', 'n', // o missing
      'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

  private SqlId() {
    throw new AssertionError("not instantiable");
  }

  /**
   * Computes Oracle sql_id of a native SQL statement.
   *
   * @param nativeSql SQL string without trailing 0x00 byte, not {@code null}
   * @return sql_id as computed by Oracle
   * @see Connection#nativeSQL(String)
   * @see OracleDatabaseException#getSql()
   */
  public static String compute(String nativeSql) {
    Objects.requireNonNull(nativeSql, "nativeSql");
    // compute the MD5 hash of the SQL
    byte[] message = nativeSql.getBytes(StandardCharsets.UTF_8);
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

  private static byte toBase32(int i) {
    if ((i < 0) || (i > 32)) {
      throw new IllegalArgumentException();
    }
    return BASE32_ALPHABET[i];
  }

}
