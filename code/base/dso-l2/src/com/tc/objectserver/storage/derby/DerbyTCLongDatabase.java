/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCLongDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

class DerbyTCLongDatabase extends AbstractDerbyTCDatabase implements TCLongDatabase {
  private final String containsQuery;
  private final String deleteQuery;
  private final String getAllQuery;
  private final String insertQuery;

  public DerbyTCLongDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    containsQuery = "SELECT " + KEY + " FROM " + tableName + " WHERE " + KEY + " = ?";
    deleteQuery = "DELETE FROM " + tableName + " WHERE " + KEY + " = ?";
    getAllQuery = "SELECT " + KEY + " FROM " + tableName;
    insertQuery = "INSERT INTO " + tableName + " (" + KEY + ") VALUES (?)";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createLongDBTable(tableName, KEY);
    executeQuery(connection, query);
  }

  public boolean contains(long key, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + KEY + " FROM " + tableName + " WHERE " + KEY
      // + " = ?"
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, containsQuery);
      psSelect.setLong(1, key);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return false; }
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }

  public Status delete(long key, PersistenceTransaction tx) {
    if (!contains(key, tx)) { return Status.NOT_FOUND; }
    try {
      // "DELETE FROM " + tableName + " WHERE " + KEY + " = ?"
      PreparedStatement psUpdate = getOrCreatePreparedStatement(tx, deleteQuery);
      psUpdate.setLong(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Set<Long> getAllKeys(PersistenceTransaction tx) {
    ResultSet rs = null;
    Set<Long> set = new HashSet<Long>();
    try {
      // "SELECT " + KEY + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, getAllQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(rs.getLong(1));
      }
      return set;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public Status insert(long key, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      // "INSERT INTO " + tableName + " VALUES (?)"
      psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, key);
      if (psPut.executeUpdate() > 0) { return Status.SUCCESS; }
    } catch (SQLException e) {
      throw new DBException(e);
    }
    throw new DBException("Could not insert with key: " + key);
  }

}
