package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = OracleConfiguration.class)
@TestConstructor(autowireMode = ALL)
class NativeSqlTests {


  private final JdbcTemplate jdbcTemplate;

  NativeSqlTests(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void nativeSql() {
    String sql = "SELECT * from dual where dummy = ?";
    String nativeSQL = this.jdbcTemplate.execute((Connection connection) -> {
      return connection.nativeSQL(sql);
    });
    assertEquals("SELECT * from dual where dummy = :1 ", nativeSQL);
    assertEquals("X", this.jdbcTemplate.queryForObject(sql, String.class, "X"));
  }

}
