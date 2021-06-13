package com.github.marschall.sqlid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 hashing as done by Oracle for SQL_ID computation.
 */
final class MD5 {

//s specifies the per-round shift amounts
  private static final byte[] S = {7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,  7, 12, 17, 22,
                                   5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,  5,  9, 14, 20,
                                   4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,  4, 11, 16, 23,
                                   6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21,  6, 10, 15, 21};

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

    // bytes 0 to 7 from the MD5 hash are not used, only the last 64bits are used
    // therefore we can use a 64bit long

    // most significant unsigned long
    return mostSignificantLong(b);
  }

  private static int F(int x, int y, int z) {
    return (x & y) | ((~x) & z);
  }

  private static int G(int x, int y, int z) {
    return (x & z) | (y & (~z));
  }

  private static int H(int x, int y, int z) {
    return (x ^ y) ^ z;
  }

  private static int I(int x, int y, int z) {
    return y ^ (x | (~z));
  }

  /* FF, GG, HH, and II transformations for rounds 1, 2, 3, and 4.
   * Rotation is separate from addition to prevent recomputation.
   */
  private static int FF(int a, int b, int c, int d, int x, int s, int ac) {
    a += F (b, c, d) + x + ac;
    a = Integer.rotateLeft(a, s);
    a += b;
    return a;
  }
  private static int GG(int a, int b, int c, int d, int x, int s, int ac) {
    a += G (b, c, d) + x + ac;
    a = Integer.rotateLeft(a, s);
    a += b;
    return a;
  }
  private static int HH(int a, int b, int c, int d, int x, int s, int ac) {
    a += H (b, c, d) + x + ac;
    a = Integer.rotateLeft(a, s);
    a += (b);
    return a;
  }
  private static int II(int a, int b, int c, int d, int x, int s, int ac) {
    a += I ((b), (c), (d)) + x + ac;
    a = Integer.rotateLeft(a, s);
    a += b;
    return a;
  }

  private static int fastWordAt(String s, int round, int index) {
    int base = (round * 64) + (4 * index);
    return s.charAt(base) << 24
        | ((s.charAt(base + 1)) << 16)
        | ((s.charAt(base + 2)) << 8)
        | (s.charAt(base + 3));
  }

  private static int slowWordAt(String s, int round, int index) {
    int base = (round * 64) + (4 * index);
    int b1 = byteValueAt(s, base + 0);
    int b2 = byteValueAt(s, base + 1);
    int b3 = byteValueAt(s, base + 2);
    int b4 = byteValueAt(s, base + 3);
    return b1 << 24
        | (b2 << 16)
        | (b3 << 8)
        | b4;
  }

  private static int byteValueAt(String s, int index) {
    int inputLenght = s.length();
    if (index < inputLenght) {
      return s.charAt(index);
    } else if (index == inputLenght) {
      // a 0 is added at the end of the SQL string for the computation of the SQL_ID
      return 0x00;
    } else if (index == inputLenght + 1) {
      // first padding byte
      return 0x01;
    }
    // past the message and first pad byte

    long messageLength = (s.length() + 1) * 8; // length in bits, additional 1 byte for the trailing 0x00 byte
    boolean finalBlock;
    if (finalBlock) {
      int indexInChunck = index / 64;
      if (indexInChunck >= 56) {
        // length in bytes, little endian
        int shift = (indexInChunck - 56) * 8;
        return (int) ((messageLength & (0xFF << shift)) >>> shift);
      } else {
        // padding
        // before the length
        return 0x00;
      }
    } else {
      // padding
      // non-final block has no length
      return 0x00;
    }
  }
  
  private static boolean needsAdditionalChunk(String s) {
    // FIXME mask
    int end = (s.length() + 1 /* 0x00 byte */ + 1 /* first pad byte */) % 64;
    return end == 0 || end > 56;
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

  private static long getUtf8Length(String s) {
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
