/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

abstract class AbstractDerbyTCDatabase {
  protected final static String KEY    = "derbykey";
  protected final static String VALUE  = "derbyvalue";

  protected final String        tableName;
  private static final TCLogger logger = TCLogging.getLogger(AbstractDerbyTCDatabase.class);

  public AbstractDerbyTCDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    this.tableName = tableName;
    try {
      createTableIfNotExists(connection, queryProvider);
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException e1) {
        throw new TCDatabaseException(e1);
      }
      throw new TCDatabaseException(e);
    }
  }

  static Connection pt2nt(PersistenceTransaction tx) {
    Object o = (tx != null) ? tx.getTransaction() : null;
    if (o != null) {
      if (!(o instanceof Connection)) { throw new AssertionError("Invalid transaction from " + tx + ": " + o); }
      return (Connection) o;
    } else {
      return null;
    }
  }

  protected void closeResultSet(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        logger.info(e.getMessage(), e);
      }
    }
  }

  protected void executeQuery(Connection connection, String query) throws SQLException {
    Statement statement = connection.createStatement();
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  protected abstract void createTableIfNotExists(Connection connection, QueryProvider queryProvider)
      throws SQLException;
}
