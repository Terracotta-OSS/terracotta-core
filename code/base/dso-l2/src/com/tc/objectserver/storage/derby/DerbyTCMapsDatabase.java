/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.derby.DerbyTCBytesToBlobDB.DerbyTCBytesBytesCursor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyTCMapsDatabase extends AbstractDerbyTCDatabase implements TCMapsDatabase {
  private static final String            OBJECT_ID = "objectid";
  private static final BackingMapFactory factory   = new BackingMapFactory() {
                                                     public Map createBackingMapFor(final ObjectID mapID) {
                                                       return new HashMap(0);
                                                     }
                                                   };

  private final String                   deleteQuery;
  private final String                   deleteCollectionBatchedQuery;
  private final String                   deleteCollectionQuery;
  private final String                   updateQuery;
  private final String                   insertQuery;
  private final String                   countQuery;
  private final String                   openCursorQuery;

  public DerbyTCMapsDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? ";
    deleteCollectionBatchedQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName + " WHERE " + OBJECT_ID
                                   + " = ?";
    deleteCollectionQuery = "DELETE FROM " + tableName + " WHERE " + OBJECT_ID + " = ?";
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ? AND " + OBJECT_ID
                  + " = ? ";
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?, ?)";
    countQuery = "SELECT COUNT(*) FROM " + tableName;
    openCursorQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName + " WHERE " + OBJECT_ID + " = ?";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createMapsDBTable(tableName, OBJECT_ID, KEY, VALUE);
    executeQuery(connection, query);
  }

  public int delete(PersistenceTransaction tx, long id, Object key, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    int written = k.length;
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? "
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setBytes(1, k);
      psUpdate.setLong(2, id);
      psUpdate.executeUpdate();
      return written;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public int deleteCollectionBatched(long id, PersistenceTransaction tx, int maxDeleteBatchSize) {
    ResultSet rs = null;
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      // + " WHERE " + OBJECT_ID + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteCollectionBatchedQuery,
                                                                ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                ResultSet.CONCUR_UPDATABLE);
      psUpdate.setLong(1, id);
      rs = psUpdate.executeQuery();
      int count = 0;
      while (rs.next() && count < maxDeleteBatchSize) {
        rs.deleteRow();
        count++;
      }

      return count;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      try {
        rs.close();
      } catch (SQLException e) {
        throw new DBException(e);
      }
    }
  }

  public void deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    try {
      // "DELETE FROM " + tableName + " WHERE " + OBJECT_ID + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteCollectionQuery);
      psUpdate.setLong(1, objectID);
      psUpdate.executeUpdate();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private int update(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? "
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, v);
      psUpdate.setBytes(2, k);
      psUpdate.setLong(3, id);
      if (psUpdate.executeUpdate() > 0) { return k.length + v.length; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not update with key: " + id);
  }

  public int update(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    final byte[] v = serializer.serialize(value);

    return update(id, k, v, tx);
  }

  private int insert(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?, ?)"
      PreparedStatement psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setBytes(2, k);
      psPut.setBytes(3, v);
      if (psPut.executeUpdate() > 0) { return k.length + v.length; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert with key: " + id);
  }

  public int insert(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    final byte[] v = serializer.serialize(value);

    return insert(id, k, v, tx);
  }

  public long count(PersistenceTransaction tx) {
    ResultSet rs = null;
    int count = 0;
    try {
      // SELECT COUNT(*) FROM tableName;
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, countQuery);
      rs = psSelect.executeQuery();

      if (rs.next()) {
        count = rs.getInt(1);
      }
      return count;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public BackingMapFactory getBackingMapFactory(TCCollectionsSerializer serializer) {
    return factory;
  }

  public void loadMap(PersistenceTransaction tx, long id, Map map, TCCollectionsSerializer serializer)
      throws TCDatabaseException {
    TCDatabaseCursor c = null;
    try {
      c = openCursor(tx, id);
      while (c.hasNext()) {
        final TCDatabaseEntry<byte[], byte[]> entry = c.next();
        final Object mkey = serializer.deserialize(entry.getKey());
        final Object mvalue = serializer.deserialize(entry.getValue());
        map.put(mkey, mvalue);
      }
    } catch (final Exception t) {
      throw new TCDatabaseException(t);
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  private TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx, long objectID) {
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      // + " WHERE " + OBJECT_ID + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, openCursorQuery);
      psSelect.setLong(1, objectID);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

}
