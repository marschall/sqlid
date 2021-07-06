package com.github.marschall.sqlid.gui;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.marschall.sqlid.SqlId;

import oracle.jdbc.pool.OracleDataSource;

final class SqlIdModel {
  
  private volatile String url;
  
  private volatile String user;
  
  private volatile String password;
  
  private volatile String query;
  
  private volatile boolean nativeSql;

  SqlIdModel() {
    super();
  }

  String getUrl() {
    return url;
  }

  void setUrl(String url) {
    this.url = url;
  }

  String getUser() {
    return user;
  }

  void setUser(String user) {
    this.user = user;
  }

  String getPassword() {
    return password;
  }

  void setPassword(String password) {
    this.password = password;
  }

  String getQuery() {
    return query;
  }

  void setQuery(String query) {
    this.query = query;
  }

  boolean isNativeSql() {
    return nativeSql;
  }

  void setNativeSql(boolean nativeSql) {
    this.nativeSql = nativeSql;
  }
  
  String computeSqlId() throws SQLException {
    String oracleSql;
    if (this.nativeSql) {
      oracleSql = this.query;
    } else {
      oracleSql = computeNativeSql();
    }
    return SqlId.compute(oracleSql);
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
  
  boolean isValid() {
    if (this.nativeSql) {
      return !isEmpty(this.query);
    } else {
      return !isEmpty(this.url)
          && !isEmpty(this.user)
          && !isEmpty(this.password)
          && !isEmpty(this.query);
    }
    
  }
  
  private static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }

}
