package com.github.marschall.sqlid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 hashing as done by Oracle for SQL_ID computation.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc1321.txt">RFC-1321</a>
 */
public final class MD5 {

  // FIXME reduce scope

//s specifies the per-round shift amounts
  private static final byte[] S = {7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
                                   5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
                                   4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
                                   6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21};

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

//Use binary integer part of the sines of integers (Radians) as constants:
  private static final int[] K = {0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
                                  0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
                                  0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
                                  0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
                                  0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
                                  0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
                                  0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
                                  0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
                                  0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
                                  0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
                                  0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
                                  0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
                                  0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
                                  0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
                                  0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
                                  0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391};

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

  public static long nonAsciiMd5HashIncrementcal(String s, MessageDigest md) {
    byte[] buffer = new byte[16];
    int bufferIndex = 0;
    int stringLength = s.length();
    for (int i = 0; i < stringLength; i++) {
      char c = s.charAt(i);
      if (Character.isLowSurrogate(c)) {
        if (i == (stringLength - 1)) {
          throw new IllegalArgumentException("truncated input");
        }
        i += 1;
        int codePoint = Character.toCodePoint(s.charAt(i), c);
      }
    }
    return 0L;
  }

  public static void main(String[] args) {
    String s = "\u1F600";
    System.out.println(s);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      System.out.println("" + i + ": " + Integer.toHexString(c));
      System.out.println("#isLowSurrogate: " + Character.isLowSurrogate(c));
      System.out.println("#isHighSurrogate: " + Character.isHighSurrogate(c));
      System.out.println("#isSurrogate: " + Character.isSurrogate(c));

    }
  }

  public static long nonAsciiMd5Hash(String s) {

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
    return mostSignificantLong(b);
  }

  public static long asciiMd5Hash(String s) {

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

  private static long getHashAscii(String s) {
    int a0 = 0x67452301; // A
    int b0 = 0xefcdab89; // B
    int c0 = 0x98badcfe; // C
    int d0 = 0x10325476; // D

    // Process the message in successive 512-bit chunks
    int fastLoopCount = s.length() / 64;
    for (int chunkIndex = 0; chunkIndex < fastLoopCount; chunkIndex++) {
      // break chunk into sixteen 32-bit words M[j], 0 ≤ j ≤ 15
      // Initialize hash value for this chunk:
      int A = a0;
      int B = b0;
      int C = c0;
      int D = d0;
      // Main loop:
      for (int i = 0; i < 63; i++) {
        int F = 0;
        int g = 0;
        if ((0 <= i) && (i <= 15)) {
          F = (B & C) | ((~ B) & D);
          g = i;
        } else if ((16 <= i) && (i <= 31)) {
          F = (D & B) | ((~ D) & C);
          g = ((5 * i) + 1) % 16;
        } else if ((32 <= i) && (i <= 47)) {
          F = B ^ C ^ D;
          g = ((3 * i) + 5) % 16;
        } else if ((48 <= i) && (i <= 63)) {
          F = C ^ (B | (~ D));
          g = (7 * i) % 16;
        }
        // Be wary of the below definitions of a,b,c,d
        // FIXME 8 bytes
        F = F + A + K[i] + s.charAt(g); // M[g] must be a 32-bits block
        A = D;
        D = C;
        C = B;
        B = B + Integer.rotateLeft(F, S[i]);
      }
      // Add this chunk's hash to result so far:
      a0 = a0 + A;
      b0 = b0 + B;
      c0 = c0 + C;
      d0 = d0 + D;
    }

    return (Integer.toUnsignedLong(c0) << 32) | d0;
  }

  private static long getHashUtf8(String s) {
    // compute the MD5 hash of the SQL
    // it's not clear whether the MD5 hash is computed based on UTF-8 or the database encoding
    byte[] message = s.getBytes(StandardCharsets.UTF_8);
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
    return mostSignificantLong(b);
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

}
