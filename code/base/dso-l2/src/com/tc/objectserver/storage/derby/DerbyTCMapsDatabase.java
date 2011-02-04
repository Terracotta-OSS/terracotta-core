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

  private final BackingMapFactory factory            = new BackingMapFactory() {
                                                       public Map createBackingMapFor(final ObjectID mapID) {
                                                         return new HashMap(0);
                                                       }
                                                     };

  public DerbyTCMapsDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
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
    Connection connection = pt2nt(tx);
    final byte[] k = serializer.serialize(id, key);
    int written = k.length;
    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, k);
      psUpdate.executeUpdate();
      return written;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public int deleteCollectionBatched(long id, PersistenceTransaction tx, int maxDeleteBatchSize) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psUpdate = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName
                                                               + " WHERE " + OBJECT_ID + " = ?",
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
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + OBJECT_ID
                                                               + " = ?");
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
    Connection connection = pt2nt(tx);
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ? AND " + OBJECT_ID + " = ? ");
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
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? ");
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
    Connection connection = pt2nt(tx);

    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?, ?)");
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
    Connection connection = pt2nt(tx);
    int count = 0;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + OBJECT_ID + ", " + KEY + " FROM "
                                                               + tableName);
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
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName
                                                               + " WHERE " + OBJECT_ID + " = ?");
      psSelect.setLong(1, objectID);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

}
