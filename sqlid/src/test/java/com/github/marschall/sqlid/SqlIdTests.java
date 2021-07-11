package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqlIdTests {

  @Test
  void sqlId() {
    String nativeSQL = "SELECT * from dual where dummy = :1 ";
    assertEquals("71hmmykrsa7wp", OriginalSqlId.SQL_ID(nativeSQL));
    assertEquals("71hmmykrsa7wp", SqlId.compute(nativeSQL));

    assertEquals("a5ks9fhw2v9s1", OriginalSqlId.SQL_ID("select * from dual"));
    assertEquals("a5ks9fhw2v9s1", SqlId.compute("select * from dual"));
  }

  @Test
  void sqlIdUmlauts() {
    String nativeSQL = "SELECT /* \u00E4 */ * from dual where dummy = :1";
    assertEquals("512k73hwcpwcx", OriginalSqlId.SQL_ID(nativeSQL));
    assertEquals("512k73hwcpwcx", SqlId.compute(nativeSQL));
  }

  @Test
  void sqlIdThreeBytes() {
    String nativeSQL = "SELECT /* \uAC00 */ * from dual where dummy = :1";
    assertEquals("bf0zf45zzqrn9", OriginalSqlId.SQL_ID(nativeSQL));
    assertEquals("bf0zf45zzqrn9", SqlId.compute(nativeSQL));
  }

  @Test
  void sqlIdFourBytes() {
    String nativeSQL = "SELECT /* \uD83D\uDC7D */ * from dual where dummy = :1";
    assertEquals("0n6qcat2kzuy0", OriginalSqlId.SQL_ID(nativeSQL));
    assertEquals("0n6qcat2kzuy0", SqlId.compute(nativeSQL));
  }

}
