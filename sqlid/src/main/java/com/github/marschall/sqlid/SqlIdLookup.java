package com.github.marschall.sqlid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import oracle.jdbc.OracleDatabaseException;

/**
 * Convenience class for computing the Oracle SQL_ID. Takes care of
 * converting form JDBC query strings to native query strings and
 * also performs caching.
 */
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
    try {
      return this.cache.get(jdbcQueryString, sql -> {
        String nativeSql;
        try (Connection connection = this.dataSource.getConnection()) {
          nativeSql = connection.nativeSQL(sql);
        } catch (SQLException e) {
          // convert checked to unchecked
          throw new UncheckedSQLException(e);
        }
        return SqlId.compute(nativeSql);
      });
    } catch (UncheckedSQLException e) {
      // convert unchecked to unchecked
      throw e.getCause();
    }
  }

  public String getSqlIdOfNativeString(String nativeSql) {
    return this.cache.get(nativeSql, SqlId::compute);
  }

  static final class UncheckedSQLException extends RuntimeException {

    UncheckedSQLException(SQLException cause) {
      super(cause);
    }

    @Override
    public synchronized SQLException getCause() {
      return (SQLException) super.getCause();
    }

  }

}
