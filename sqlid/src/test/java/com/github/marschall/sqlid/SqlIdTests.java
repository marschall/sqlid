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

}
