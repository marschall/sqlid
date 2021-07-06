package com.github.marschall.sqlid.gui;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.swing.SwingWorker;

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

  SwingWorker<String, String> computeSqlIdWworker() {
    return new SqlIdWorker(this.newSqlIdComputation());
  }

  private AbstractSqlIdComputation newSqlIdComputation() {
    if (this.nativeSql) {
      return new NativeSqlIdComputation(this.query);
    } else {
      return new JdbcSqlIdComputation(this.url, this.user, this.password, this.query);
    }
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
