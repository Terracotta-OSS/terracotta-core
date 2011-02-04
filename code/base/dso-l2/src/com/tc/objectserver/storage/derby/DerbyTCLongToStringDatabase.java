/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;

import gnu.trove.TLongObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class DerbyTCLongToStringDatabase extends AbstractDerbyTCDatabase implements TCLongToStringDatabase {

  public DerbyTCLongToStringDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createLongToStringDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        target.put(rs.getLong(1), rs.getString(2));
      }
      return target;
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

  public Status put(long id, String string, PersistenceTransaction tx) {
    if (get(id, tx) == null) { return insert(id, string, tx); }
    return Status.NOT_SUCCESS;
  }

  private Status insert(long id, String b, PersistenceTransaction tx) {
    PreparedStatement psPut;
    Connection connection = pt2nt(tx);

    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setLong(1, id);
      psPut.setString(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return Status.SUCCESS;
  }

  private byte[] get(long id, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setLong(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }
}
