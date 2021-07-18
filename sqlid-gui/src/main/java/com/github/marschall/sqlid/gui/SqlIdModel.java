package com.github.marschall.sqlid.gui;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

final class SqlIdModel {

  private static final String PASSWORD_PREFERENCE = "password";

  private static final String USER_PREFERENCE = "user";

  private static final String URL_PREFERENCE = "url";

  private volatile String url;

  private volatile String user;

  private volatile String password;

  private volatile String query;

  private volatile boolean nativeSql;

  private Preferences preferences;

  SqlIdModel() {
    super();
  }

  String getUrl() {
    return this.url;
  }

  void setUrl(String url) {
    this.url = url;
    this.preferences.put(URL_PREFERENCE, url);
  }

  String getUser() {
    return this.user;
  }

  void setUser(String user) {
    this.user = user;
    this.preferences.put(USER_PREFERENCE, user);
  }

  String getPassword() {
    return this.password;
  }

  void setPassword(String password) {
    this.password = password;
    this.preferences.put(PASSWORD_PREFERENCE, password);
  }

  String getQuery() {
    return this.query;
  }

  void setQuery(String query) {
    this.query = query;
  }

  boolean isNativeSql() {
    return this.nativeSql;
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

  void load() {
    this.preferences = Preferences.userNodeForPackage(SqlIdModel.class);
    this.url = this.preferences.get(URL_PREFERENCE, null);
    this.user = this.preferences.get(USER_PREFERENCE, null);
    this.password = this.preferences.get(PASSWORD_PREFERENCE, null);
  }

  void flush() throws BackingStoreException {
    this.preferences.flush();
  }

  private static boolean isEmpty(String s) {
    return (s == null) || s.isEmpty();
  }

}
