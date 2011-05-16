/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCIntToBytesDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyTCIntToBytesDatabase extends AbstractDerbyTCDatabase implements TCIntToBytesDatabase {
  private final String getQuery;
  private final String getAllQuery;
  private final String updateQuery;
  private final String insertQuery;

  public DerbyTCIntToBytesDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    getQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
    getAllQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName;
    updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createIntToBytesDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public byte[] get(int id, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getQuery);
      psSelect.setInt(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  public Map<Integer, byte[]> getAll(PersistenceTransaction tx) {
    ResultSet rs = null;
    Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getAllQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getInt(1), rs.getBytes(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public Status put(int id, byte[] b, PersistenceTransaction tx) {
    if (get(id, tx) == null) {
      return insert(id, b, tx);
    } else {
      return update(id, b, tx);
    }
  }

  public Status update(int id, byte[] b, PersistenceTransaction tx) {
    try {
      // "UPDATE " + tableName + " SET " + VALUE + " = ? "
      // + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, updateQuery);
      psUpdate.setBytes(1, b);
      psUpdate.setInt(2, id);
      if (psUpdate.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not update with id: " + id);
  }

  public Status insert(int id, byte[] b, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setInt(1, id);
      psPut.setBytes(2, b);
      if (psPut.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert with id: " + id);
  }

}
