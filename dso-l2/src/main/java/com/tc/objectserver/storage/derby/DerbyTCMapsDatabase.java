/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.BatchedTransaction;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyTCMapsDatabase extends AbstractDerbyTCDatabase implements TCMapsDatabase {
  private static final TCLogger          logger                 = TCLogging.getLogger(DerbyTCMapsDatabase.class);
  private static final String            MAP_ID                 = "mapid";
  private static final String            BIG_KEY                = "bigkey";
  private static final int               DERBY_VARCHAR_LIMIT    = 16000;
  private static final int               KEY_HASH_SIZE          = 4;
  private static final int               SEQUENCE_ID_SIZE       = 4;
  private static final int               SMALL_KEY_MAX_LEN      = DERBY_VARCHAR_LIMIT - KEY_HASH_SIZE
                                                                  - SEQUENCE_ID_SIZE;
  private static final int               SEQUENCE_WARNING_LIMIT = 10;
  private static final BackingMapFactory factory                = new BackingMapFactory() {
                                                                  public Map createBackingMapFor(final ObjectID mapID) {
                                                                    return new HashMap(0);
                                                                  }
                                                                };

  private final String                   deleteQuery;
  private final String                   deleteCollectionQuery;
  private final String                   updateQuery;
  private final String                   insertBigKeyQuery;
  private final String                   insertSmallKeyQuery;
  private final String                   countQuery;
  private final String                   openCursorQuery;
  private final String                   updateBigKeyQuery;

  public DerbyTCMapsDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + MAP_ID + " = ? AND " + KEY + " = ? ";
    deleteCollectionQuery = "DELETE FROM " + tableName + " WHERE " + MAP_ID + " = ?";
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + MAP_ID + " = ? AND " + KEY + " = ?";
    insertBigKeyQuery = "SELECT * FROM " + tableName + " WHERE " + MAP_ID + " = ? AND " + KEY + " >= ? AND " + KEY
                        + " <= ?";
    insertSmallKeyQuery = "INSERT INTO " + tableName + "(" + MAP_ID + ", " + KEY + ", " + VALUE + ") VALUES (?, ?, ?)";
    countQuery = "SELECT COUNT(" + KEY + ") FROM " + tableName;
    openCursorQuery = "SELECT " + KEY + "," + BIG_KEY + ", " + VALUE + " FROM " + tableName + " WHERE " + MAP_ID
                      + " = ?";
    updateBigKeyQuery = "SELECT " + BIG_KEY + ", " + VALUE + " FROM " + tableName + " WHERE " + MAP_ID + " = ? AND "
                        + KEY + " >= ? AND " + KEY + " <= ?";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createMapsDBTable(tableName, MAP_ID, KEY, BIG_KEY, VALUE);
    executeQuery(connection, query);
  }

  public int delete(PersistenceTransaction tx, long mapId, Object key, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    int written = k.length;
    try {
      if (isBigKey(k)) { return deleteBigKey(mapId, k, tx); }
      // "DELETE FROM " + tableName + " WHERE " + MAP_ID + " = ? AND " + KEY + " = ? "
      PreparedStatement psDelete = getOrCreatePreparedStatement(tx, deleteQuery);
      psDelete.setLong(1, mapId);
      psDelete.setBytes(2, k);
      if (psDelete.executeUpdate() > 0) {
        return written;
      } else {
        return 0;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private int deleteBigKey(long mapId, byte[] bigKey, PersistenceTransaction tx) throws SQLException {
    ResultSet updateSet = updatableKeyQuery(mapId, bigKey, tx);
    try {
      while (updateSet.next()) {
        if (isDBKeyEqual(updateSet.getBlob(1), bigKey)) {
          updateSet.deleteRow();
          return bigKey.length;
        }
      }
      return 0;
    } finally {
      closeResultSet(updateSet);
    }
  }

  public void deleteCollectionBatched(long mapId, BatchedTransaction batchedTransaction) throws TCDatabaseException {
    PersistenceTransaction tx = batchedTransaction.getCurrentTransaction();
    int deleteCollectionCount = 0;
    try {
      deleteCollectionCount = deleteCollection(mapId, tx);
    } finally {
      batchedTransaction.optionalCommit(deleteCollectionCount);
    }
  }

  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    try {
      // "DELETE FROM " + tableName + " WHERE " + OBJECT_ID + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteCollectionQuery);
      psUpdate.setLong(1, objectID);
      return psUpdate.executeUpdate();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private int update(long mapId, byte[] key, byte[] value, PersistenceTransaction tx) {
    try {
      if (isBigKey(key)) { return updateBigKey(mapId, key, value, tx); }
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + MAP_ID + " = ? AND " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, value);
      psUpdate.setLong(2, mapId);
      psUpdate.setBytes(3, key);
      if (psUpdate.executeUpdate() > 0) { return key.length + value.length; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not update with map: " + mapId);
  }

  private int updateBigKey(long mapId, byte[] bigKey, byte[] value, PersistenceTransaction tx) throws SQLException {
    ResultSet updateSet = updatableKeyQuery(mapId, bigKey, tx);
    try {
      while (updateSet.next()) {
        if (isDBKeyEqual(updateSet.getBlob(1), bigKey)) {
          updateSet.updateBytes(2, value);
          updateSet.updateRow();
          return bigKey.length + value.length;
        }
      }
      return 0;
    } finally {
      closeResultSet(updateSet);
    }
  }

  public int update(PersistenceTransaction tx, long mapId, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    final byte[] v = serializer.serialize(value);
    return update(mapId, k, v, tx);
  }

  private int insert(long mapId, byte[] key, byte[] value, PersistenceTransaction tx) {
    try {
      if (isBigKey(key)) { return insertBigKey(mapId, key, value, tx); }
      // INSERT INTO table (MAP_ID, SMALL_KEY, VALUE) VALUES(?, ?, ?)
      PreparedStatement psInsert = getOrCreatePreparedStatement(tx, insertSmallKeyQuery);
      psInsert.setLong(1, mapId);
      psInsert.setBytes(2, key);
      psInsert.setBytes(3, value);
      if (psInsert.executeUpdate() > 0) { return key.length + value.length; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert into map: " + mapId);
  }

  private int insertBigKey(long mapId, byte[] bigKey, byte[] value, PersistenceTransaction tx) throws SQLException {
    // SELECT * FROM mapsdatabase WHERE MAP_ID = ? AND KEY >= ? AND KEY <= ?
    PreparedStatement psInsert = getOrCreatePreparedStatement(tx, insertBigKeyQuery, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                              ResultSet.CONCUR_UPDATABLE);
    psInsert.setLong(1, mapId);
    psInsert.setBytes(2, abbreviateKey(bigKey, 0));
    psInsert.setBytes(3, abbreviateKey(bigKey, Integer.MAX_VALUE));
    ResultSet insertSet = psInsert.executeQuery();
    try {
      int availableSequenceId = 0;
      int collisions = 0;
      while (insertSet.next()) {
        if (availableSequenceId == getSequenceIdFromAbbreviatedKey(insertSet.getBytes(KEY))) {
          availableSequenceId++;
        }
        if (isDBKeyEqual(insertSet.getBlob(BIG_KEY), bigKey)) { throw new DBException("Duplicate key insert into map "
                                                                                      + mapId); }
        collisions++;
        if ((collisions % SEQUENCE_WARNING_LIMIT == 0)) {
          logger.warn("Big Keys - High hash collision rate in map " + mapId);
        }
      }
      insertSet.moveToInsertRow();
      insertSet.updateLong(MAP_ID, mapId);
      insertSet.updateBytes(KEY, abbreviateKey(bigKey, availableSequenceId));
      insertSet.updateBinaryStream(BIG_KEY, new ByteArrayInputStream(bigKey, SMALL_KEY_MAX_LEN, bigKey.length
                                                                                                - SMALL_KEY_MAX_LEN),
                                   bigKey.length - SMALL_KEY_MAX_LEN);
      insertSet.updateBytes(VALUE, value);
      insertSet.insertRow();
      return bigKey.length + value.length;
    } finally {
      closeResultSet(insertSet);
    }
  }

  public int insert(PersistenceTransaction tx, long mapId, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException {
    final byte[] k = serializer.serialize(key);
    final byte[] v = serializer.serialize(value);
    return insert(mapId, k, v, tx);
  }

  public long count(PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT COUNT(" + OBJECT_ID + ") FROM "
      // + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, countQuery);
      rs = psSelect.executeQuery();

      if (rs.next()) { return rs.getLong(1); }
      return 0;
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
    ResultSet rs = null;
    try {
      // SELECT KEY, BIG_KEY, VALUE FROM mapsdatabase WHERE MAP_ID = ?
      PreparedStatement psLoad = getOrCreatePreparedStatement(tx, openCursorQuery);
      psLoad.setLong(1, id);
      rs = psLoad.executeQuery();
      while (rs.next()) {
        byte[] smallKey = rs.getBytes(1);
        Object mkey = null;
        if (!isBigKey(smallKey)) {
          mkey = serializer.deserialize(smallKey);
        } else {
          byte[] bigKeyTail = rs.getBytes(2);
          byte[] bigKey = new byte[SMALL_KEY_MAX_LEN + bigKeyTail.length];
          System.arraycopy(smallKey, 0, bigKey, 0, SMALL_KEY_MAX_LEN);
          System.arraycopy(bigKeyTail, 0, bigKey, SMALL_KEY_MAX_LEN, bigKeyTail.length);
          mkey = serializer.deserialize(bigKey);
        }
        final Object mvalue = serializer.deserialize(rs.getBytes(3));
        map.put(mkey, mvalue);
      }
    } catch (final Exception t) {
      throw new TCDatabaseException(t);
    } finally {
      closeResultSet(rs);
    }
  }

  private ResultSet updatableKeyQuery(long mapId, byte[] bigKey, PersistenceTransaction tx) throws SQLException {
    // SELECT KEY, BIG_KEY, VALUE FROM mapsdb WHERE MAP_ID = ? AND KEY >= ? AND KEY <= ?
    PreparedStatement psUpdatableKeyQuery = getOrCreatePreparedStatement(tx, updateBigKeyQuery,
                                                                         ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                         ResultSet.CONCUR_UPDATABLE);
    psUpdatableKeyQuery.setLong(1, mapId);
    psUpdatableKeyQuery.setBytes(2, abbreviateKey(bigKey, 0));
    psUpdatableKeyQuery.setBytes(3, abbreviateKey(bigKey, Integer.MAX_VALUE));
    return psUpdatableKeyQuery.executeQuery();
  }

  private byte[] hashKey(byte[] key) {
    int hash = 0;
    for (byte element : key) {
      hash = 31 * hash + element;
    }
    return Conversion.int2Bytes(hash);
  }

  private boolean isDBKeyEqual(Blob candidateBigKeyTail, byte[] bigKey) throws SQLException {
    InputStream blobInputStream = candidateBigKeyTail.getBinaryStream();
    try {
      // Only verifying the remaining bytes of the key since the first SMALL_KEY_MAX_LEN bytes
      // should already be handled by the DB for the SELECT.
      for (int i = SMALL_KEY_MAX_LEN; i < bigKey.length; i++) {
        int blobValue = blobInputStream.read();
        if (blobValue == -1 || ((byte) blobValue) != bigKey[i]) { return false; }
      }
      if (blobInputStream.read() != -1) { return false; }
      return true;
    } catch (IOException e) {
      throw new DBException(e);
    }
  }

  private boolean isBigKey(byte[] key) {
    return key.length > SMALL_KEY_MAX_LEN;
  }

  private byte[] abbreviateKey(byte[] key, int sequenceId) {
    Assert.eval(isBigKey(key));
    byte[] smallKey = new byte[DERBY_VARCHAR_LIMIT];
    System.arraycopy(key, 0, smallKey, 0, SMALL_KEY_MAX_LEN);
    System.arraycopy(hashKey(key), 0, smallKey, SMALL_KEY_MAX_LEN, KEY_HASH_SIZE);
    System
        .arraycopy(Conversion.int2Bytes(sequenceId), 0, smallKey, SMALL_KEY_MAX_LEN + KEY_HASH_SIZE, SEQUENCE_ID_SIZE);
    return smallKey;
  }

  private int getSequenceIdFromAbbreviatedKey(byte[] abbreviatedKey) {
    return Conversion.bytes2Int(abbreviatedKey, SMALL_KEY_MAX_LEN + KEY_HASH_SIZE);
  }
}
