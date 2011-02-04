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

class DerbyTCBytesToBlobDB extends AbstractDerbyTCDatabase implements TCBytesToBytesDatabase {
  public DerbyTCBytesToBlobDB(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createBytesToBlobDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public Status delete(byte[] key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
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
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName,
                                                               ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                               ResultSet.CONCUR_UPDATABLE);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
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

  private Status update(byte[] key, byte[] val, PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, val);
      psUpdate.setBytes(2, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private Status insert(byte[] key, byte[] val, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      Connection connection = pt2nt(tx);
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setBytes(1, key);
      psPut.setBytes(2, val);
      psPut.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
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