package com.github.marschall.sqlid;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 hashing as done by Oracle for SQL_ID computation.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc1321.txt">RFC-1321</a>
 */
final class MD5 {

  /*
   * Constants for MD5Transform routine.
   */
  private static final int S11 = 7;
  private static final int S12 = 12;
  private static final int S13 = 17;
  private static final int S14 = 22;
  private static final int S21 = 5;
  private static final int S22 = 9;
  private static final int S23 = 14;
  private static final int S24 = 20;
  private static final int S31 = 4;
  private static final int S32 = 11;
  private static final int S33 = 16;
  private static final int S34 = 23;
  private static final int S41 = 6;
  private static final int S42 = 10;
  private static final int S43 = 15;
  private static final int S44 = 21;

  private MD5() {
    throw new AssertionError("not instantiable");
  }

  static long getBinarySqlId(String s) {
    // compute the MD5 hash of the SQL
    int utf8Length = getUtf8Length(s);
    if (utf8Length == s.length()) {
      return asciiMd5Hash(s);
    } else {
      return nonAsciiMd5Hash(s);
    }
  }

  static long nonAsciiMd5Hash(String s) {
    Hasher hasher = new Hasher();
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i++);
      if (Character.isHighSurrogate(c)) {
        if (i == s.length()) {
          throw new IllegalArgumentException("malformed input, truncated");
        }
        char low = s.charAt(i++);
        if (!Character.isLowSurrogate(low)) {
          throw new IllegalArgumentException("malformed input, missing surrogate pair");
        }
        int codePoint = Character.toCodePoint(c, low);
        hasher.put((byte) (0b11110000 | ((codePoint & 0b111_000000_000000_000000) >>> 18)));
        hasher.put((byte) (0b10000000 | ((codePoint & 0b111111_000000_000000) >>> 12)));
        hasher.put((byte) (0b10000000 | ((codePoint & 0b111111_000000) >>> 6)));
        hasher.put((byte) (0b10000000 | (codePoint & 0b111111)));
      } else {
        if (c < 0b10000000) {
          hasher.put((byte) c);
        } else if (c < 0b100000000000) {
          hasher.put((byte) (0b11000000 | ((c & 0b11111_000000) >>> 6)));
          hasher.put((byte) (0b10000000 | (c & 0b111111)));
        } else {
          hasher.put((byte) (0b11100000 | ((c & 0b1111_000000_000000) >>> 12)));
          hasher.put((byte) (0b10000000 | ((c & 0b111111_000000) >>> 6)));
          hasher.put((byte) (0b10000000 | (c & 0b111111)));
        }
      }
    }
    return hasher.finish();
  }

  static long asciiMd5Hash(String s) {

    int a0 = 0x67452301; // A
    int b0 = 0xefcdab89; // B
    int c0 = 0x98badcfe; // C
    int d0 = 0x10325476; // D

    // Process the message in successive 512-bit chunks
    // we can directly index into the message string
    int fastLoopCount = s.length() / 64;
    int totalLoopCount = fastLoopCount + (needsAdditionalChunk(s) ? 2 : 1);

    for (int chunkIndex = 0; chunkIndex < totalLoopCount; chunkIndex++) {
      int a = a0;
      int b = b0;
      int c = c0;
      int d = d0;

      int  x0;
      int  x1;
      int  x2;
      int  x3;
      int  x4;
      int  x5;
      int  x6;
      int  x7;
      int  x8;
      int  x9;
      int x10;
      int x11;
      int x12;
      int x13;
      int x14;
      int x15;

      if (chunkIndex < fastLoopCount) {
         x0 = fastWordAt(s,  0, chunkIndex);
         x1 = fastWordAt(s,  1, chunkIndex);
         x2 = fastWordAt(s,  2, chunkIndex);
         x3 = fastWordAt(s,  3, chunkIndex);
         x4 = fastWordAt(s,  4, chunkIndex);
         x5 = fastWordAt(s,  5, chunkIndex);
         x6 = fastWordAt(s,  6, chunkIndex);
         x7 = fastWordAt(s,  7, chunkIndex);
         x8 = fastWordAt(s,  8, chunkIndex);
         x9 = fastWordAt(s,  9, chunkIndex);
        x10 = fastWordAt(s, 10, chunkIndex);
        x11 = fastWordAt(s, 11, chunkIndex);
        x12 = fastWordAt(s, 12, chunkIndex);
        x13 = fastWordAt(s, 13, chunkIndex);
        x14 = fastWordAt(s, 14, chunkIndex);
        x15 = fastWordAt(s, 15, chunkIndex);
      } else {
        boolean isLast = chunkIndex == (totalLoopCount - 1);
         x0 = slowWordAt(s,  0, chunkIndex, isLast);
         x1 = slowWordAt(s,  1, chunkIndex, isLast);
         x2 = slowWordAt(s,  2, chunkIndex, isLast);
         x3 = slowWordAt(s,  3, chunkIndex, isLast);
         x4 = slowWordAt(s,  4, chunkIndex, isLast);
         x5 = slowWordAt(s,  5, chunkIndex, isLast);
         x6 = slowWordAt(s,  6, chunkIndex, isLast);
         x7 = slowWordAt(s,  7, chunkIndex, isLast);
         x8 = slowWordAt(s,  8, chunkIndex, isLast);
         x9 = slowWordAt(s,  9, chunkIndex, isLast);
        x10 = slowWordAt(s, 10, chunkIndex, isLast);
        x11 = slowWordAt(s, 11, chunkIndex, isLast);
        x12 = slowWordAt(s, 12, chunkIndex, isLast);
        x13 = slowWordAt(s, 13, chunkIndex, isLast);
        x14 = slowWordAt(s, 14, chunkIndex, isLast);
        x15 = slowWordAt(s, 15, chunkIndex, isLast);
        if (isLast) {
          long messageLength = (s.length() + 1) * 8; // length in bits, additional 1 byte for the trailing 0x00 byte
          x14 = (int) messageLength;
          x15 = (int) (messageLength >>> 32);
        }
      }

      // fully inline FF, GG, HH and II

      /* Round 1 */
      a = Integer.rotateLeft(a + ((b & c) | ((~b) & d)) + x0 + 0xd76aa478, S11) + b; /* 1 */
      d = Integer.rotateLeft(d + ((a & b) | ((~a) & c)) + x1 + 0xe8c7b756, S12) + a; /* 2 */
      c = Integer.rotateLeft(c + ((d & a) | ((~d) & b)) + x2 + 0x242070db, S13) + d; /* 3 */
      b = Integer.rotateLeft(b + ((c & d) | ((~c) & a)) + x3 + 0xc1bdceee, S14) + c; /* 4 */
      a = Integer.rotateLeft(a + ((b & c) | ((~b) & d)) + x4 + 0xf57c0faf, S11) + b; /* 5 */
      d = Integer.rotateLeft(d + ((a & b) | ((~a) & c)) + x5 + 0x4787c62a, S12) + a; /* 6 */
      c = Integer.rotateLeft(c + ((d & a) | ((~d) & b)) + x6 + 0xa8304613, S13) + d; /* 7 */
      b = Integer.rotateLeft(b + ((c & d) | ((~c) & a)) + x7 + 0xfd469501, S14) + c; /* 8 */
      a = Integer.rotateLeft(a + ((b & c) | ((~b) & d)) + x8 + 0x698098d8, S11) + b; /* 9 */
      d = Integer.rotateLeft(d + ((a & b) | ((~a) & c)) + x9 + 0x8b44f7af, S12) + a; /* 10 */
      c = Integer.rotateLeft(c + ((d & a) | ((~d) & b)) + x10 + 0xffff5bb1, S13) + d; /* 11 */
      b = Integer.rotateLeft(b + ((c & d) | ((~c) & a)) + x11 + 0x895cd7be, S14) + c; /* 12 */
      a = Integer.rotateLeft(a + ((b & c) | ((~b) & d)) + x12 + 0x6b901122, S11) + b; /* 13 */
      d = Integer.rotateLeft(d + ((a & b) | ((~a) & c)) + x13 + 0xfd987193, S12) + a; /* 14 */
      c = Integer.rotateLeft(c + ((d & a) | ((~d) & b)) + x14 + 0xa679438e, S13) + d; /* 15 */
      b = Integer.rotateLeft(b + ((c & d) | ((~c) & a)) + x15 + 0x49b40821, S14) + c; /* 16 */

      /* Round 2 */
      a = Integer.rotateLeft(a + ((b & d) | (c & (~d))) + x1 + 0xf61e2562, S21) + b; /* 17 */
      d = Integer.rotateLeft(d + ((a & c) | (b & (~c))) + x6 + 0xc040b340, S22) + a; /* 18 */
      c = Integer.rotateLeft(c + ((d & b) | (a & (~b))) + x11 + 0x265e5a51, S23) + d; /* 19 */
      b = Integer.rotateLeft(b + ((c & a) | (d & (~a))) + x0 + 0xe9b6c7aa, S24) + c; /* 20 */
      a = Integer.rotateLeft(a + ((b & d) | (c & (~d))) + x5 + 0xd62f105d, S21) + b; /* 21 */
      d = Integer.rotateLeft(d + ((a & c) | (b & (~c))) + x10 + 0x2441453, S22) + a; /* 22 */
      c = Integer.rotateLeft(c + ((d & b) | (a & (~b))) + x15 + 0xd8a1e681, S23) + d; /* 23 */
      b = Integer.rotateLeft(b + ((c & a) | (d & (~a))) + x4 + 0xe7d3fbc8, S24) + c; /* 24 */
      a = Integer.rotateLeft(a + ((b & d) | (c & (~d))) + x9 + 0x21e1cde6, S21) + b; /* 25 */
      d = Integer.rotateLeft(d + ((a & c) | (b & (~c))) + x14 + 0xc33707d6, S22) + a; /* 26 */
      c = Integer.rotateLeft(c + ((d & b) | (a & (~b))) + x3 + 0xf4d50d87, S23) + d; /* 27 */
      b = Integer.rotateLeft(b + ((c & a) | (d & (~a))) + x8 + 0x455a14ed, S24) + c; /* 28 */
      a = Integer.rotateLeft(a + ((b & d) | (c & (~d))) + x13 + 0xa9e3e905, S21) + b; /* 29 */
      d = Integer.rotateLeft(d + ((a & c) | (b & (~c))) + x2 + 0xfcefa3f8, S22) + a; /* 30 */
      c = Integer.rotateLeft(c + ((d & b) | (a & (~b))) + x7 + 0x676f02d9, S23) + d; /* 31 */
      b = Integer.rotateLeft(b + ((c & a) | (d & (~a))) + x12 + 0x8d2a4c8a, S24) + c; /* 32 */

      /* Round 3 */
      a = Integer.rotateLeft(a + ((b ^ c) ^ d) + x5 + 0xfffa3942, S31) + b; /* 33 */
      d = Integer.rotateLeft(d + ((a ^ b) ^ c) + x8 + 0x8771f681, S32) + a; /* 34 */
      c = Integer.rotateLeft(c + ((d ^ a) ^ b) + x11 + 0x6d9d6122, S33) + d; /* 35 */
      b = Integer.rotateLeft(b + ((c ^ d) ^ a) + x14 + 0xfde5380c, S34) + c; /* 36 */
      a = Integer.rotateLeft(a + ((b ^ c) ^ d) + x1 + 0xa4beea44, S31) + b; /* 37 */
      d = Integer.rotateLeft(d + ((a ^ b) ^ c) + x4 + 0x4bdecfa9, S32) + a; /* 38 */
      c = Integer.rotateLeft(c + ((d ^ a) ^ b) + x7 + 0xf6bb4b60, S33) + d; /* 39 */
      b = Integer.rotateLeft(b + ((c ^ d) ^ a) + x10 + 0xbebfbc70, S34) + c; /* 40 */
      a = Integer.rotateLeft(a + ((b ^ c) ^ d) + x13 + 0x289b7ec6, S31) + b; /* 41 */
      d = Integer.rotateLeft(d + ((a ^ b) ^ c) + x0 + 0xeaa127fa, S32) + a; /* 42 */
      c = Integer.rotateLeft(c + ((d ^ a) ^ b) + x3 + 0xd4ef3085, S33) + d; /* 43 */
      b = Integer.rotateLeft(b + ((c ^ d) ^ a) + x6 + 0x4881d05, S34) + c; /* 44 */
      a = Integer.rotateLeft(a + ((b ^ c) ^ d) + x9 + 0xd9d4d039, S31) + b; /* 45 */
      d = Integer.rotateLeft(d + ((a ^ b) ^ c) + x12 + 0xe6db99e5, S32) + a; /* 46 */
      c = Integer.rotateLeft(c + ((d ^ a) ^ b) + x15 + 0x1fa27cf8, S33) + d; /* 47 */
      b = Integer.rotateLeft(b + ((c ^ d) ^ a) + x2 + 0xc4ac5665, S34) + c; /* 48 */

      /* Round 4 */
      a = Integer.rotateLeft(a + (c ^ (b | (~d))) + x0 + 0xf4292244, S41) + b; /* 49 */
      d = Integer.rotateLeft(d + (b ^ (a | (~c))) + x7 + 0x432aff97, S42) + a; /* 50 */
      c = Integer.rotateLeft(c + (a ^ (d | (~b))) + x14 + 0xab9423a7, S43) + d; /* 51 */
      b = Integer.rotateLeft(b + (d ^ (c | (~a))) + x5 + 0xfc93a039, S44) + c; /* 52 */
      a = Integer.rotateLeft(a + (c ^ (b | (~d))) + x12 + 0x655b59c3, S41) + b; /* 53 */
      d = Integer.rotateLeft(d + (b ^ (a | (~c))) + x3 + 0x8f0ccc92, S42) + a; /* 54 */
      c = Integer.rotateLeft(c + (a ^ (d | (~b))) + x10 + 0xffeff47d, S43) + d; /* 55 */
      b = Integer.rotateLeft(b + (d ^ (c | (~a))) + x1 + 0x85845dd1, S44) + c; /* 56 */
      a = Integer.rotateLeft(a + (c ^ (b | (~d))) + x8 + 0x6fa87e4f, S41) + b; /* 57 */
      d = Integer.rotateLeft(d + (b ^ (a | (~c))) + x15 + 0xfe2ce6e0, S42) + a; /* 58 */
      c = Integer.rotateLeft(c + (a ^ (d | (~b))) + x6 + 0xa3014314, S43) + d; /* 59 */
      b = Integer.rotateLeft(b + (d ^ (c | (~a))) + x13 + 0x4e0811a1, S44) + c; /* 60 */
      a = Integer.rotateLeft(a + (c ^ (b | (~d))) + x4 + 0xf7537e82, S41) + b; /* 61 */
      d = Integer.rotateLeft(d + (b ^ (a | (~c))) + x11 + 0xbd3af235, S42) + a; /* 62 */
      c = Integer.rotateLeft(c + (a ^ (d | (~b))) + x2 + 0x2ad7d2bb, S43) + d; /* 63 */
      b = Integer.rotateLeft(b + (d ^ (c | (~a))) + x9 + 0xeb86d391, S44) + c; /* 64 */

      a0 += a;
      b0 += b;
      c0 += c;
      d0 += d;
    }

    return (Integer.toUnsignedLong(c0) << 32) | Integer.toUnsignedLong(d0);
  }

  /**
   * Quick access to a word in the input message, does not deal with padding.
   */
  private static int fastWordAt(String s, int index, int chunckIndex) {
    int base = (chunckIndex * 64) + (4 * index);
    return s.charAt(base)
        | ((s.charAt(base + 1)) << 8)
        | ((s.charAt(base + 2)) << 16)
        | ((s.charAt(base + 3)) << 24);
  }

  /**
   * Slow access to a word in the input message, deals with padding.
   */
  private static int slowWordAt(String s, int index, int chunkIndex, boolean finalBlock) {
    int base = (chunkIndex * 64) + (4 * index);
    int b1 = byteValueAt(s, base + 0, finalBlock);
    int b2 = byteValueAt(s, base + 1, finalBlock);
    int b3 = byteValueAt(s, base + 2, finalBlock);
    int b4 = byteValueAt(s, base + 3, finalBlock);
    return b1
        | (b2 << 8)
        | (b3 << 16)
        | (b4 << 24);
  }

  private static int byteValueAt(String s, int index, boolean finalBlock) {
    int inputLenght = s.length();
    if (index < inputLenght) {
      return s.charAt(index);
    } else if (index == inputLenght) {
      // a 0 is added at the end of the SQL string for the computation of the SQL_ID
      return 0x00;
    } else if (index == (inputLenght + 1)) {
      // first padding byte
      return 0x80;
    }
    // past the message and first pad byte

    return 0x00;
  }

  private static boolean needsAdditionalChunk(String s) {
    // FIXME mask
    int end = s.length() % 64;
    return end > (56 - 1 /* 0x00 byte */ - 1 /* first pad byte */);
  }

  private static int getUtf8Length(String s) {
    int length = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isLowSurrogate(c)) {
        if (c <= 0b1111111) {
          length += 1;
        } else if (c <= 0b11111_111111) {
            length += 2;
        } else if (c <= 0b1111_111111_111111) {
          length += 3;
        } else {
          length += 4;
        }
      }

    }
    return length;
  }

  static final class Hasher {

    static final int CHUNK_SIZE = 64;
    static final int HASH_SIZE = 16;

    private final MessageDigest messageDigest;
    private final byte[] buffer;
    private int position;

    Hasher() {
      try {
        this.messageDigest = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("MD5 not supported", e);
      }
      this.buffer = new byte[CHUNK_SIZE];
      this.position = 0;
    }

    void put(byte b) {
      this.buffer[this.position++] = b;
      if (this.position == CHUNK_SIZE) {
        this.messageDigest.update(this.buffer);
        this.position = 0;
      }
    }

    long finish() {
      // append a trailing 0x00 byte
      this.put((byte) 0x00);
      if (this.position != 0) {
        this.messageDigest.update(this.buffer, 0, this.position);
      }
      int resultSize;
      try {
        // reuse the buffer already allocated
        resultSize = this.messageDigest.digest(this.buffer, 0, HASH_SIZE);
      } catch (DigestException e) {
        throw new IllegalStateException("MD5 could not be computed", e);
      }
      if (resultSize != HASH_SIZE) {
        throw new IllegalStateException("Unexpected result size: " +resultSize);
      }

      // bytes 0 - 7 from the hash are not used, only the last 64bits are used
      // therefore we can use a 64bit long
      return mostSignificantLong(this.buffer);
    }
  }

  static long mostSignificantLong(byte[] b) {
    if (b.length < 16) {
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

}
