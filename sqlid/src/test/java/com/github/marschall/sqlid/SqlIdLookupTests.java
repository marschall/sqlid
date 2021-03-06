package com.github.marschall.sqlid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class SqlIdLookupTests extends AbstractOracleTests {

  private final SqlIdLookup lookup;
  private final DataSource dataSource;

  SqlIdLookupTests(DataSource dataSource) {
    this.dataSource = dataSource;
    this.lookup = new SqlIdLookup(dataSource, 3);
  }

  @Test
  void getSqlIdOfJdbcString() throws SQLException {
    String sqlId1 = this.lookup.getSqlIdOfJdbcString("SELECT * from dual where dummy = ?");
    assertEquals("71hmmykrsa7wp", sqlId1);
    String sqlId2 = this.lookup.getSqlIdOfJdbcString("SELECT * from dual where dummy = ?");
    assertSame(sqlId1, sqlId2);

    assertEquals("a5ks9fhw2v9s1", this.lookup.getSqlIdOfJdbcString("select * from dual"));
  }

  @Test
  void getSqlIdOfNativeString() throws SQLException {
    String sqlId1 = this.lookup.getSqlIdOfNativeString("SELECT * from dual where dummy = :1 ");
    assertEquals("71hmmykrsa7wp", sqlId1);
    String sqlId2 = this.lookup.getSqlIdOfNativeString("SELECT * from dual where dummy = :1 ");
    assertSame(sqlId1, sqlId2);

    assertEquals("a5ks9fhw2v9s1", this.lookup.getSqlIdOfNativeString("select * from dual"));
  }

  @Test
  void tgetSqlIdOfException() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from dual where dummy = ?")) {
      preparedStatement.setObject(1, LocalDate.of(2021, 6, 7));
      SQLException sqlException = assertThrows(SQLException.class, () -> preparedStatement.executeQuery());
      Optional<String> maybeSqlId = this.lookup.getSqlIdOfException(sqlException);
      assertTrue(maybeSqlId.isPresent());
      assertEquals("71hmmykrsa7wp", maybeSqlId.get());
    }
  }

  @Test
  void tgetSqlIdOfExceptionNoSql() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         Statement statement = connection.createStatement(-1, -1)) {
      SQLException sqlException = assertThrows(SQLException.class, () -> statement.getMoreResults(-1));
      Optional<String> maybeSqlId = this.lookup.getSqlIdOfException(sqlException);
      assertFalse(maybeSqlId.isPresent());

      sqlException = assertThrows(SQLException.class, () -> statement.setMaxFieldSize(-1));
      maybeSqlId = this.lookup.getSqlIdOfException(sqlException);
      assertFalse(maybeSqlId.isPresent());
    }
  }

  @Test
  void throwException() throws SQLException {

    DataSource throwingDataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(throwingDataSource.getConnection()).thenReturn(connection);

    SQLException expectedException = new SQLException("nativeSQL not available");
    when(connection.nativeSQL(anyString())).thenThrow(expectedException);

    SqlIdLookup throwingLookup = new SqlIdLookup(throwingDataSource, 1);
    SQLException actualException = assertThrows(SQLException.class, () -> throwingLookup.getSqlIdOfJdbcString("SELECT * from dual where dummy = ?"));

    assertSame(expectedException, actualException);
  }

}
