/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class DerbyTCTransactionStoreDatabase extends AbstractDerbyTCDatabase implements TCTransactionStoreDatabase {
  private final String deleteQuery;
  private final String insertQuery;
  private final String openCursorQuery;

  public DerbyTCTransactionStoreDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    openCursorQuery = "SELECT " + KEY + ", " + VALUE + " FROM " + tableName;
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createObjectDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public Status delete(long id, PersistenceTransaction tx) {
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setLong(1, id);
      if (psUpdate.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status insert(long id, byte[] value, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setBytes(2, value);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

  public TCDatabaseCursor<Long, byte[]> openCursor(PersistenceTransaction tx) {
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, openCursorQuery);
      return new DerbyTCLongToBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  static class DerbyTCLongToBytesCursor implements TCDatabaseCursor<Long, byte[]> {
    private final ResultSet rs;

    public DerbyTCLongToBytesCursor(ResultSet rs) {
      this.rs = rs;
    }

    private TCDatabaseEntry<Long, byte[]> entry    = null;
    private boolean                       finished = false;

    public boolean hasNext() {
      if (entry != null) { return true; }
      if (finished) { return false; }

      boolean hasNext = false;
      try {
        hasNext = rs.next();
        if (hasNext) {
          entry = new TCDatabaseEntry<Long, byte[]>();
          entry.setKey(rs.getLong(1)).setValue(rs.getBytes(2));
        }
      } catch (SQLException e) {
        throw new DBException(e);
      }

      if (!hasNext) {
        finished = true;
      }
      return hasNext;
    }

    public TCDatabaseEntry<Long, byte[]> next() {
      if (entry == null) { throw new DBException("next call should be called only after checking hasNext."); }
      TCDatabaseEntry<Long, byte[]> temp = entry;
      entry = null;
      return temp;
    }

    public void close() {
      try {
        rs.close();
      } catch (SQLException e) {
        throw new DBException(e);
      }
    }

    public void delete() {
      try {
        rs.deleteRow();
      } catch (SQLException e) {
        throw new DBException(e);
      }
    }
  }
}
