package com.github.marschall.sqlid;

import java.sql.Connection;
import java.util.Objects;

import oracle.jdbc.OracleDatabaseException;

/**
 * Computes Oracle sql_id of a SQL statement.
 *
 * @see <a href="https://tanelpoder.com/2009/02/22/sql_id-is-just-a-fancy-representation-of-hash-value/">SQL_ID is just a fancy representation of hash value</a>
 * @see <a href="https://web.archive.org/web/20170510061149/http://www.slaviks-blog.com/2010/03/30/oracle-sql_id-and-hash-value/">Oracle sql_id and hash value</a>
 */
public final class SqlId {

  private SqlId() {
    throw new AssertionError("not instantiable");
  }

  /**
   * Computes Oracle sql_id of a native SQL statement.
   *
   * @param nativeSql SQL string without trailing 0x00 byte, not {@code null}
   * @return sql_id as computed by Oracle
   * @see Connection#nativeSQL(String)
   * @see OracleDatabaseException#getSql()
   */
  public static String compute(String nativeSql) {
    Objects.requireNonNull(nativeSql, "nativeSql");
    long id = MD5.getBinarySqlId(nativeSql);
    return Base32.toBase32String(id);
  }

}
