package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.benmanes.caffeine.cache.Caffeine;

class SqlIdLookupDifferentCacheTests {

  private static final String JDBC_QUERY = "SELECT * from dual where dummy = ?";

  private static final String NATIVE_QUERY = "SELECT * from dual where dummy = :1 ";

  private DataSource dataSource;

  @BeforeEach
  void setUp() throws SQLException {
    this.dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(this.dataSource.getConnection()).thenReturn(connection);
    when(connection.nativeSQL(JDBC_QUERY)).thenReturn(NATIVE_QUERY);
  }

  @Test
  void noCache() throws SQLException {
    SqlIdLookup lookup = new SqlIdLookup(this.dataSource, (key, loader) -> loader.apply(key));
    String sqlId1 = lookup.getSqlIdOfJdbcString(JDBC_QUERY);
    String sqlId2 = lookup.getSqlIdOfJdbcString(JDBC_QUERY);
    assertEquals(sqlId1, sqlId2);
    assertNotSame(sqlId1, sqlId2);
  }

  static List<Cache<String, String>> caches() {
    com.github.benmanes.caffeine.cache.Cache<String, String> caffeine = Caffeine.newBuilder()
      .maximumSize(16)
      .build();
    Cache<String, String> caffeineCache = (key, loader) -> caffeine.get(key, k -> loader.apply(k));
    return List.of(caffeineCache);
  }

  @ParameterizedTest
  @MethodSource("caches")
  void otherCaches(Cache<String, String> cache) throws SQLException {
    SqlIdLookup lookup = new SqlIdLookup(this.dataSource, cache);
    String sqlId1 = lookup.getSqlIdOfJdbcString(JDBC_QUERY);
    String sqlId2 = lookup.getSqlIdOfJdbcString(JDBC_QUERY);
    assertEquals(sqlId1, sqlId2);
    assertSame(sqlId1, sqlId2);
  }

}
