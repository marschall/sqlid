package com.github.marschall.sqlid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import oracle.jdbc.OracleDatabaseException;

public final class SqlIdLookup {

  private final DataSource dataSource;

  public SqlIdLookup(DataSource dataSource) {
    Objects.requireNonNull(dataSource, "dataSource");
    this.dataSource = dataSource;
  }

  public Optional<String> getSqlId(SQLException sqlException) {
    Throwable cause = sqlException.getCause();
    if (cause instanceof OracleDatabaseException) {
      String originalSql = ((OracleDatabaseException) cause).getOriginalSql();
      return Optional.of(this.getSqlIdOfNativeString(originalSql));
    } else {
      return Optional.empty();
    }
  }

  public String getSqlIdOfJdbcString(String jdbcQueryString) throws SQLException {
    String nativeSql;
    try (Connection connection = this.dataSource.getConnection()) {
      nativeSql = connection.nativeSQL(jdbcQueryString);
    }
    return this.getSqlIdOfNativeString(nativeSql);
  }

  public String getSqlIdOfNativeString(String nativeSql) {
    return SqlId.SQL_ID(nativeSql);
  }

}
