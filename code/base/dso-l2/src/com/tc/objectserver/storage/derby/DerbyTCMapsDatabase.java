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
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.derby.DerbyTCBytesToBlobDB.DerbyTCBytesBytesCursor;
import com.tc.util.Conversion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyTCMapsDatabase extends AbstractDerbyTCDatabase implements TCMapsDatabase {
  private static final String     OBJECT_ID          = "objectid";
  private static final String     INDEX_OBJECTID     = "indexMapObjectId";
  private static final String     INDEX_OBJECTID_KEY = "indexMapObjectIdKey";

  private final String            deleteQuery;
  private final String            deleteCollectionBatchedQuery;
  private final String            deleteCollectionQuery;
  private final String            containsQuery;
  private final String            updateQuery;
  private final String            insertQuery;
  private final String            countQuery;
  private final String            openCursorQuery;

  private final BackingMapFactory factory            = new BackingMapFactory() {
                                                       public Map createBackingMapFor(final ObjectID mapID) {
                                                         return new HashMap(0);
                                                       }
                                                     };

  public DerbyTCMapsDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    deleteCollectionBatchedQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName + " WHERE " + OBJECT_ID
                                   + " = ?";
    deleteCollectionQuery = "DELETE FROM " + tableName + " WHERE " + OBJECT_ID + " = ?";
    containsQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? ";
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ? AND " + OBJECT_ID
                  + " = ? ";
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?, ?)";
    countQuery = "SELECT " + OBJECT_ID + ", " + KEY + " FROM " + tableName;
    openCursorQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName + " WHERE " + OBJECT_ID + " = ?";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createMapsDBTable(tableName, OBJECT_ID, KEY, VALUE);
    executeQuery(connection, query);

    query = queryProvider.createMapsDBIndexObjectID(INDEX_OBJECTID, tableName, OBJECT_ID, KEY, VALUE);
    executeQuery(connection, query);

    query = queryProvider.createMapsDBIndexObjectdIDKey(INDEX_OBJECTID_KEY, tableName, OBJECT_ID, KEY, VALUE);
    executeQuery(connection, query);
  }

  public int delete(PersistenceTransaction tx, long id, Object key, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(id, key);
    int written = k.length;
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setBytes(1, k);
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

  public int put(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(id, key);
    final byte[] v = serializer.serialize(value);
    final int written = v.length + k.length;

    if (!contains(id, k, tx)) {
      insert(id, k, v, tx);
    } else {
      update(id, k, v, tx);
    }
    return written;
  }

  private boolean contains(long id, byte[] k, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE "
      // + KEY + " = ? AND " + OBJECT_ID + " = ? "
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, containsQuery);
      psSelect.setBytes(1, k);
      psSelect.setLong(2, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return false; }
      return true;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  private Status update(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? "
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, v);
      psUpdate.setBytes(2, k);
      psUpdate.setLong(3, id);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private Status insert(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?, ?)"
      PreparedStatement psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setBytes(2, k);
      psPut.setBytes(3, v);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

  public long count(PersistenceTransaction tx) {
    ResultSet rs = null;
    int count = 0;
    try {
      // "SELECT " + OBJECT_ID + ", " + KEY + " FROM "
      // + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, countQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        count++;
      }
      tx.commit();
      return count;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }

  public BackingMapFactory getBackingMapFactory(TCCollectionsSerializer serializer) {
    return factory;
  }

  public void loadMap(PersistenceTransaction tx, long id, Map map, TCCollectionsSerializer serializer)
      throws TCDatabaseException {
    final byte idb[] = Conversion.long2Bytes(id);
    TCDatabaseCursor c = null;
    try {
      c = openCursor(tx, id);
      while (c.hasNext()) {
        final TCDatabaseEntry<byte[], byte[]> entry = c.next();
        final Object mkey = serializer.deserialize(idb.length, entry.getKey());
        final Object mvalue = serializer.deserialize(entry.getValue());
        map.put(mkey, mvalue);
      }
    } catch (final Exception t) {
      throw new TCDatabaseException(t.getMessage());
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
