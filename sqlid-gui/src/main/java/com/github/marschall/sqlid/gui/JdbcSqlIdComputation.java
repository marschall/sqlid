package com.github.marschall.sqlid.gui;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.marschall.sqlid.SqlId;

import oracle.jdbc.pool.OracleDataSource;

final class JdbcSqlIdComputation extends AbstractSqlIdComputation {

  private final String url;
  private final String user;
  private final String password;

  JdbcSqlIdComputation(String url, String user, String password, String query) {
    super(query);
    this.url = url;
    this.user = user;
    this.password = password;
  }

  @Override
  String computeSqlId() throws SQLException {
    return SqlId.compute(this.computeNativeSql());
  }

  private String computeNativeSql() throws SQLException {
    DataSource dataSource = createDataSource();
    try (Connection connection = dataSource.getConnection()) {
      return connection.nativeSQL(this.query);
    }
  }

  private DataSource createDataSource() throws SQLException {
    OracleDataSource dataSource = new OracleDataSource();
    dataSource.setURL(this.url);
    dataSource.setUser(this.user);
    dataSource.setPassword(this.password);
    return dataSource;
  }

}
