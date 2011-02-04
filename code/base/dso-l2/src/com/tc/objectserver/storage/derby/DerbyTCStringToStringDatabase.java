/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseEntry;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class DerbyTCStringToStringDatabase extends AbstractDerbyTCDatabase implements TCStringToStringDatabase {

  public DerbyTCStringToStringDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createStringToStringDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public Status delete(String key, PersistenceTransaction tx) {
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    entry.setKey(key);
    Status status = get(entry, tx);
    if (status != Status.SUCCESS) { return status; }

    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setString(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status get(TCDatabaseEntry<String, String> entry, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setString(1, entry.getKey());
      rs = psSelect.executeQuery();

      if (!rs.next()) { return Status.NOT_FOUND; }
      entry.setValue(rs.getString(1));

      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }

  public Status put(String key, String value, PersistenceTransaction tx) {
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    entry.setKey(key);
    Status status = get(entry, tx);
    if (status == Status.SUCCESS) {
      return update(key, value, tx);
    } else {
      return insert(key, value, tx);
    }

  }

  private Status update(String key, String value, PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);

      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setString(1, value);
      psUpdate.setString(2, key);
      if (psUpdate.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_SUCCESS;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private Status insert(String key, String value, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      Connection connection = pt2nt(tx);

      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setString(1, key);
      psPut.setString(2, value);
      if (psPut.executeUpdate() > 0) {
        return Status.SUCCESS;
      } else {
        return Status.NOT_SUCCESS;
      }
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }
}
