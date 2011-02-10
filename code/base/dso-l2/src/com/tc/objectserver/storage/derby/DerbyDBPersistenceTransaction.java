/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyDBPersistenceTransaction implements PersistenceTransaction {
  private final Connection                                     connection;
  private final Map<String, PreparedStatement>                 cachedPreparedStatements          = new HashMap<String, PreparedStatement>();
  private final Map<PreparedStatementQuery, PreparedStatement> cachedPreparedStatementsForCursor = new HashMap<PreparedStatementQuery, PreparedStatement>();

  public DerbyDBPersistenceTransaction(Connection connection) {
    this.connection = connection;
  }

  public void abort() {
    try {
      connection.rollback();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void commit() {
    try {
      connection.commit();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public DerbyDBPersistenceTransaction getTransaction() {
    return this;
  }

  public PreparedStatement getOrCreatePrepartedStatement(String query) throws SQLException {
    PreparedStatement preparedStatement = cachedPreparedStatements.get(query);
    if (preparedStatement == null) {
      preparedStatement = connection.prepareStatement(query);
      cachedPreparedStatements.put(query, preparedStatement);
    }
    return preparedStatement;
  }

  public PreparedStatement getOrCreatePrepartedStatement(String query, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    PreparedStatementQuery preparedStatementQuery = new PreparedStatementQuery(query, resultSetType,
                                                                               resultSetConcurrency);
    PreparedStatement preparedStatement = cachedPreparedStatementsForCursor.get(preparedStatementQuery);
    if (preparedStatement == null) {
      preparedStatement = connection.prepareStatement(query, resultSetType, resultSetConcurrency);
      cachedPreparedStatementsForCursor.put(preparedStatementQuery, preparedStatement);
    }
    return preparedStatement;
  }

  private static class PreparedStatementQuery {
    private final int    resultSetType;
    private final int    resultSetConcurrency;
    private final String query;

    public PreparedStatementQuery(String query, int resultSetType, int resultSetConcurrency) {
      this.resultSetType = resultSetType;
      this.resultSetConcurrency = resultSetConcurrency;
      this.query = query;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((query == null) ? 0 : query.hashCode());
      result = prime * result + resultSetConcurrency;
      result = prime * result + resultSetType;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      PreparedStatementQuery other = (PreparedStatementQuery) obj;
      if (query == null) {
        if (other.query != null) return false;
      } else if (!query.equals(other.query)) return false;
      if (resultSetConcurrency != other.resultSetConcurrency) return false;
      if (resultSetType != other.resultSetType) return false;
      return true;
    }

  }
}
