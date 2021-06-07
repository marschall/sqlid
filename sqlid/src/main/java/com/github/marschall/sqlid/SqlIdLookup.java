package com.github.marschall.sqlid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import oracle.jdbc.OracleDatabaseException;

public final class SqlIdLookup {

  private final DataSource dataSource;
  private final Cache<String, String> cache;

  public SqlIdLookup(DataSource dataSource, Cache<String, String> cache) {
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(cache, "cache");
    this.dataSource = dataSource;
    this.cache = cache;
  }

  public SqlIdLookup(DataSource dataSource, int cacheCapacity) {
    if (cacheCapacity < 0) {
      throw new IllegalArgumentException("cache capacity must be positive but was: " + cacheCapacity);
    }
    this.dataSource = dataSource;
    this.cache = new HashLruCache<>(cacheCapacity);
  }

  public Optional<String> getSqlIdOfException(SQLException sqlException) {
    Throwable cause = sqlException.getCause();
    if (cause instanceof OracleDatabaseException) {
      // #getOriginalSql() returns the JDBC string
      String originalSql = ((OracleDatabaseException) cause).getSql();
      return Optional.of(this.getSqlIdOfNativeString(originalSql));
    } else {
      return Optional.empty();
    }
  }

  public String getSqlIdOfJdbcString(String jdbcQueryString) throws SQLException {
    // TODO cache
    String nativeSql;
    try (Connection connection = this.dataSource.getConnection()) {
      nativeSql = connection.nativeSQL(jdbcQueryString);
    }
    return SqlId.computeSqlId(nativeSql);
  }

  public String getSqlIdOfNativeString(String nativeSql) {
    return this.cache.get(nativeSql, SqlId::computeSqlId);
  }

}
