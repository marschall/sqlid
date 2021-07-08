package com.github.marschall.sqlid.gui;

import java.sql.SQLException;

abstract class AbstractSqlIdComputation {

  protected final String query;

  AbstractSqlIdComputation(String query) {
    this.query = query;
  }

  abstract String computeSqlId() throws SQLException;

}
