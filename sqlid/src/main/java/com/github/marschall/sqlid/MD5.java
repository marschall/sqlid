package com.github.marschall.sqlid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 hashing as done by Oracle for SQL_ID computation.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc1321.txt">RFC-1321</a>
 */
final class MD5 {

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
  
  static long nonAsciiMd5Hash(String s) {

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
      }

      /* Round 1 */
      a = FF(a, b, c, d,  x0, S11, 0xd76aa478); /* 1 */
      d = FF(d, a, b, c,  x1, S12, 0xe8c7b756); /* 2 */
      c = FF(c, d, a, b,  x2, S13, 0x242070db); /* 3 */
      b = FF(b, c, d, a,  x3, S14, 0xc1bdceee); /* 4 */
      a = FF(a, b, c, d,  x4, S11, 0xf57c0faf); /* 5 */
      d = FF(d, a, b, c,  x5, S12, 0x4787c62a); /* 6 */
      c = FF(c, d, a, b,  x6, S13, 0xa8304613); /* 7 */
      b = FF(b, c, d, a,  x7, S14, 0xfd469501); /* 8 */
      a = FF(a, b, c, d,  x8, S11, 0x698098d8); /* 9 */
      d = FF(d, a, b, c,  x9, S12, 0x8b44f7af); /* 10 */
      c = FF(c, d, a, b, x10, S13, 0xffff5bb1); /* 11 */
      b = FF(b, c, d, a, x11, S14, 0x895cd7be); /* 12 */
      a = FF(a, b, c, d, x12, S11, 0x6b901122); /* 13 */
      d = FF(d, a, b, c, x13, S12, 0xfd987193); /* 14 */
      c = FF(c, d, a, b, x14, S13, 0xa679438e); /* 15 */
      b = FF(b, c, d, a, x15, S14, 0x49b40821); /* 16 */

      /* Round 2 */
      a = GG(a, b, c, d,  x1, S21, 0xf61e2562); /* 17 */
      d = GG(d, a, b, c,  x6, S22, 0xc040b340); /* 18 */
      c = GG(c, d, a, b, x11, S23, 0x265e5a51); /* 19 */
      b = GG(b, c, d, a,  x0, S24, 0xe9b6c7aa); /* 20 */
      a = GG(a, b, c, d,  x5, S21, 0xd62f105d); /* 21 */
      d = GG(d, a, b, c, x10, S22,  0x2441453); /* 22 */
      c = GG(c, d, a, b, x15, S23, 0xd8a1e681); /* 23 */
      b = GG(b, c, d, a,  x4, S24, 0xe7d3fbc8); /* 24 */
      a = GG(a, b, c, d,  x9, S21, 0x21e1cde6); /* 25 */
      d = GG(d, a, b, c, x14, S22, 0xc33707d6); /* 26 */
      c = GG(c, d, a, b,  x3, S23, 0xf4d50d87); /* 27 */
      b = GG(b, c, d, a,  x8, S24, 0x455a14ed); /* 28 */
      a = GG(a, b, c, d, x13, S21, 0xa9e3e905); /* 29 */
      d = GG(d, a, b, c,  x2, S22, 0xfcefa3f8); /* 30 */
      c = GG(c, d, a, b,  x7, S23, 0x676f02d9); /* 31 */
      b = GG(b, c, d, a, x12, S24, 0x8d2a4c8a); /* 32 */

      /* Round 3 */
      a = HH(a, b, c, d,  x5, S31, 0xfffa3942); /* 33 */
      d = HH(d, a, b, c,  x8, S32, 0x8771f681); /* 34 */
      c = HH(c, d, a, b, x11, S33, 0x6d9d6122); /* 35 */
      b = HH(b, c, d, a, x14, S34, 0xfde5380c); /* 36 */
      a = HH(a, b, c, d,  x1, S31, 0xa4beea44); /* 37 */
      d = HH(d, a, b, c,  x4, S32, 0x4bdecfa9); /* 38 */
      c = HH(c, d, a, b,  x7, S33, 0xf6bb4b60); /* 39 */
      b = HH(b, c, d, a, x10, S34, 0xbebfbc70); /* 40 */
      a = HH(a, b, c, d, x13, S31, 0x289b7ec6); /* 41 */
      d = HH(d, a, b, c,  x0, S32, 0xeaa127fa); /* 42 */
      c = HH(c, d, a, b,  x3, S33, 0xd4ef3085); /* 43 */
      b = HH(b, c, d, a,  x6, S34,  0x4881d05); /* 44 */
      a = HH(a, b, c, d,  x9, S31, 0xd9d4d039); /* 45 */
      d = HH(d, a, b, c, x12, S32, 0xe6db99e5); /* 46 */
      c = HH(c, d, a, b, x15, S33, 0x1fa27cf8); /* 47 */
      b = HH(b, c, d, a,  x2, S34, 0xc4ac5665); /* 48 */

      /* Round 4 */
      a = II(a, b, c, d,  x0, S41, 0xf4292244); /* 49 */
      d = II(d, a, b, c,  x7, S42, 0x432aff97); /* 50 */
      c = II(c, d, a, b, x14, S43, 0xab9423a7); /* 51 */
      b = II(b, c, d, a,  x5, S44, 0xfc93a039); /* 52 */
      a = II(a, b, c, d, x12, S41, 0x655b59c3); /* 53 */
      d = II(d, a, b, c,  x3, S42, 0x8f0ccc92); /* 54 */
      c = II(c, d, a, b, x10, S43, 0xffeff47d); /* 55 */
      b = II(b, c, d, a,  x1, S44, 0x85845dd1); /* 56 */
      a = II(a, b, c, d,  x8, S41, 0x6fa87e4f); /* 57 */
      d = II(d, a, b, c, x15, S42, 0xfe2ce6e0); /* 58 */
      c = II(c, d, a, b,  x6, S43, 0xa3014314); /* 59 */
      b = II(b, c, d, a, x13, S44, 0x4e0811a1); /* 60 */
      a = II(a, b, c, d,  x4, S41, 0xf7537e82); /* 61 */
      d = II(d, a, b, c, x11, S42, 0xbd3af235); /* 62 */
      c = II(c, d, a, b,  x2, S43, 0x2ad7d2bb); /* 63 */
      b = II(b, c, d, a,  x9, S44, 0xeb86d391); /* 64 */

      a0 += a;
      b0 += b;
      c0 += c;
      d0 += d;
    }

    return (Integer.toUnsignedLong(c0) << 32) | Integer.toUnsignedLong(d0);
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

    // FIXME * 8 << 3
    long messageLength = (s.length() + 1) * 8; // length in bits, additional 1 byte for the trailing 0x00 byte
    if (finalBlock) {
      if (index >= 56) {
        // length in bytes, little endian
        int shift = (index - 56) * 8;
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
    return (end == 0) || (end > 56);
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
