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

  public DerbyTCIntToBytesDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createIntToBytesDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public byte[] get(int id, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
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
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getInt(1), rs.getBytes(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      try {
        closeResultSet(rs);
        connection.commit();
      } catch (SQLException e) {
        // Ignore
      }
    }
  }

  public Status put(int id, byte[] b, PersistenceTransaction tx) {
    if (get(id, tx) == null) {
      return insert(id, b, tx);
    } else {
      return update(id, b, tx);
    }
  }

  private Status update(int id, byte[] b, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, b);
      psUpdate.setInt(2, id);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private Status insert(int id, byte[] b, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setInt(1, id);
      psPut.setBytes(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

}
