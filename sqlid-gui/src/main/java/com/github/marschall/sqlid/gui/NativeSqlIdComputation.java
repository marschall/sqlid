package com.github.marschall.sqlid.gui;

import com.github.marschall.sqlid.SqlId;

final class NativeSqlIdComputation extends AbstractSqlIdComputation {

  NativeSqlIdComputation(String query) {
    super(query);
  }

  @Override
  String computeSqlId() {
    return SqlId.compute(this.query);
  }

}
