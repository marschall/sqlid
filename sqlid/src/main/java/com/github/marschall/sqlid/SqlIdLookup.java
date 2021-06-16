package com.github.marschall.sqlid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import oracle.jdbc.OracleDatabaseException;

/**
 * Convenience class for computing the Oracle sql_id. Takes care of
 * converting form JDBC query strings to native query strings and
 * also performs caching.
 */
public final class SqlIdLookup {

  private final DataSource dataSource;
  private final Cache<String, String> cache;

  /**
   * Constructs a new {@link SqlIdLookup} with the given cache.
   * 
   * @param dataSource the data source must directly or indirectly be an Oracle data source,
   *                   not {@code null}
   * @param cache the cache to use,
   *              not {@code null}
   */
  public SqlIdLookup(DataSource dataSource, Cache<String, String> cache) {
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(cache, "cache");
    this.dataSource = dataSource;
    this.cache = cache;
  }

  /**
   * Constructs a new {@link SqlIdLookup} with an LRU cache of the given capacity.
   * 
   * @param dataSource the data source must directly or indirectly be an Oracle data source,
   *                   not {@code null}
   * @param cacheCapacity the capacity or the LRU cache,
   *                      must be positive
   * @see HashLruCache
   */
  public SqlIdLookup(DataSource dataSource, int cacheCapacity) {
    Objects.requireNonNull(dataSource, "dataSource");
    if (cacheCapacity < 0) {
      throw new IllegalArgumentException("cache capacity must be positive but was: " + cacheCapacity);
    }
    this.dataSource = dataSource;
    this.cache = new HashLruCache<>(cacheCapacity);
  }

  /**
   * Computes the sql_id of a statement that caused the given exception.
   * <p>
   * For this method to work the Oracle driver JAR must be visible to this class.
   * 
   * @param sqlException the SQL exception raised from the Oracle driver
   * @return a present optional with the sql_id if the cause of the given exception was a {@link OracleDatabaseException},
   *         an empty optional if the cause of the given exception was not a {@link OracleDatabaseException}
   */
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

  /**
   * Computes the sql_id of a JDBC query string with ? as place holders for bind parameters.
   * 
   * @param jdbcQueryString the JDBC query string with ? as place holder,
   *                        not {@code null}
   * @return the Oracle sql_id of {@code jdbcQueryString}
   * @throws SQLException if no connection can be acquired or {@link Connection#nativeSQL(String)}
   *                      throws a {@link SQLException}
   */
  public String getSqlIdOfJdbcString(String jdbcQueryString) throws SQLException {
    Objects.requireNonNull(jdbcQueryString, "jdbcQueryString");
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

  /**
   * Computes the sql_id of a native Oracle query string with named place holders for bind parameters, eg :value1.
   * 
   * @param nativeSql the native Oracle query string with named place holders for bind parameters, eg :value1,
   *                  not {@code null}
   * @return the Oracle sql_id of {@code nativeSql}
   */
  public String getSqlIdOfNativeString(String nativeSql) {
    Objects.requireNonNull(nativeSql, "nativeSql");
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
