package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class SqlIdLookupDifferentCacheTests extends AbstractOracleTests {

  private final DataSource dataSource;

  SqlIdLookupDifferentCacheTests(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Test
  void noCache() throws SQLException {
    SqlIdLookup lookup = new SqlIdLookup(this.dataSource, (key, loader) -> loader.apply(key));
    String sqlId1 = lookup.getSqlIdOfJdbcString("SELECT * from dual where dummy = ?");
    String sqlId2 = lookup.getSqlIdOfJdbcString("SELECT * from dual where dummy = ?");
    assertEquals(sqlId1, sqlId2);
    assertNotSame(sqlId1, sqlId2);
  }

}
