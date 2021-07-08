package com.github.marschall.sqlid.gui;

import javax.swing.SwingWorker;

final class SqlIdWorker extends SwingWorker<String, String> {

  private final AbstractSqlIdComputation computation;

  SqlIdWorker(AbstractSqlIdComputation computation) {
    this.computation = computation;
  }

  @Override
  protected String doInBackground() throws Exception {
    return this.computation.computeSqlId();
  }

}
