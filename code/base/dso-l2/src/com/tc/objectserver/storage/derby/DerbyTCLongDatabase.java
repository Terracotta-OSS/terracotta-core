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

  public DerbyTCLongDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createLongDBTable(tableName, KEY);
    executeQuery(connection, query);
  }

  public boolean contains(long key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName + " WHERE " + KEY
                                                               + " = ?");
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
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
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
    Connection connection = pt2nt(tx);
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(rs.getLong(1));
      }
      return set;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // Ignore
      }
    }
  }

  public Status put(long key, PersistenceTransaction tx) {
    if (contains(key, tx)) {
      return Status.NOT_SUCCESS;
    } else {
      return insert(key, tx);
    }
  }

  private Status insert(long key, PersistenceTransaction tx) {
    PreparedStatement psPut;
    Connection connection = pt2nt(tx);
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?)");
      psPut.setLong(1, key);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

}
