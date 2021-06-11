package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class NativeSqlTests extends AbstractOracleTests {

  private final JdbcTemplate jdbcTemplate;

  NativeSqlTests(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void nativeSql() {
    String sql = "SELECT * from dual where dummy = ?";
    String nativeSql = this.jdbcTemplate.execute((Connection connection) -> {
      return connection.nativeSQL(sql);
    });


    assertEquals("SELECT * from dual where dummy = :1 ", nativeSql);
    assertEquals(nativeSql, nativeSql);
    assertEquals("X", this.jdbcTemplate.queryForObject(sql, String.class, "X"));
    String sqlText = this.jdbcTemplate.queryForObject("SELECT sql_text FROM v$sqltext WHERE sql_id = ?", String.class, "71hmmykrsa7wp");
    assertEquals(nativeSql, sqlText);
  }

}
