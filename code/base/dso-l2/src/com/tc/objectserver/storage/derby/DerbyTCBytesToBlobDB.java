/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

class DerbyTCBytesToBlobDB extends AbstractDerbyTCDatabase implements TCBytesToBytesDatabase {
  private final String deleteQuery;
  private final String getQuery;
  private final String openCursorQuery;
  private final String updateQuery;
  private final String insertQuery;

  public DerbyTCBytesToBlobDB(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    getQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
    openCursorQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName;
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
    insertQuery = "INSERT INTO " + tableName + " (" + KEY + ", " + VALUE + ") VALUES (?, ?)";
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createBytesToBlobDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public Status delete(byte[] key, PersistenceTransaction tx) {
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setBytes(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getQuery);
      psSelect.setBytes(1, key);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx) {
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName, ResultSet.TYPE_SCROLL_INSENSITIVE,
      // ResultSet.CONCUR_UPDATABLE
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, openCursorQuery, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                ResultSet.CONCUR_UPDATABLE);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx) {
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, openCursorQuery);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status put(byte[] key, byte[] val, PersistenceTransaction tx) {
    if (get(key, tx) == null) {
      return insert(key, val, tx);
    } else {
      return update(key, val, tx);
    }
  }

  public Status update(byte[] key, byte[] val, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, val);
      psUpdate.setBytes(2, key);
      if (psUpdate.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not update with key: " + Arrays.toString(key));
  }

  public Status insert(byte[] key, byte[] val, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setBytes(1, key);
      psPut.setBytes(2, val);
      if (psPut.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert with key: " + Arrays.toString(key));
  }

  public Status putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value) {
    if (get(key, tx) == null) { return insert(key, value, tx); }
    return Status.NOT_SUCCESS;
  }

  static class DerbyTCBytesBytesCursor implements TCDatabaseCursor<byte[], byte[]> {
    private final ResultSet rs;

    public DerbyTCBytesBytesCursor(ResultSet rs) {
      this.rs = rs;
    }

    private TCDatabaseEntry<byte[], byte[]> entry    = null;
    private boolean                         finished = false;

    public boolean hasNext() {
      if (entry != null) { return true; }
      if (finished) { return false; }

      boolean hasNext = false;
      try {
        hasNext = rs.next();
        if (hasNext) {
          entry = new TCDatabaseEntry<byte[], byte[]>();
          entry.setKey(rs.getBytes(1)).setValue(rs.getBytes(2));
        }
      } catch (SQLException e) {
        throw new DBException(e);
      }

      if (!hasNext) {
        finished = true;
      }
      return hasNext;
    }

    public TCDatabaseEntry<byte[], byte[]> next() {
      if (entry == null) { throw new DBException("next call should be called only after checking hasNext."); }
      TCDatabaseEntry<byte[], byte[]> temp = entry;
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