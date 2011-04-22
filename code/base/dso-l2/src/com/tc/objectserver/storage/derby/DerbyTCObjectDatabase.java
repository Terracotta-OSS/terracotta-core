/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCObjectDatabase;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.ObjectIDSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class DerbyTCObjectDatabase extends AbstractDerbyTCDatabase implements TCObjectDatabase {
  private static final TCLogger logger = TCLogging.getLogger(DerbyTCObjectDatabase.class);
  private final SampledCounter  l2FaultFromDisk;

  private final String          deleteQuery;
  private final String          getQuery;
  private final String          updateQuery;
  private final String          insertQuery;
  private final String          getAllObjectIDsQuery;

  public DerbyTCObjectDatabase(String tableName, Connection connection, QueryProvider queryProvider,
                               SampledCounter l2FaultFromDisk) throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    this.l2FaultFromDisk = l2FaultFromDisk;
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    getQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
    getAllObjectIDsQuery = "SELECT " + KEY + " FROM " + tableName;
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
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

  public byte[] get(long id, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getQuery);
      psSelect.setLong(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      l2FaultFromDisk.increment();
      return temp;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  public ObjectIDSet getAllObjectIds(PersistenceTransaction tx) {
    ResultSet rs = null;
    ObjectIDSet set = new ObjectIDSet();
    try {
      // "SELECT " + KEY + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getAllObjectIDsQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(new ObjectID(rs.getLong(1)));
      }
      return set;
    } catch (Throwable e) {
      logger.error("Error Reading Object IDs", e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
    return null;
  }

  public Status update(long id, byte[] b, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, b);
      psUpdate.setLong(2, id);
      if (psUpdate.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status insert(long id, byte[] b, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setBytes(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

  public Status put(long id, byte[] b, PersistenceTransaction tx) {
    if (update(id, b, tx) == Status.SUCCESS) { return Status.SUCCESS; }
    return insert(id, b, tx);
  }
}
