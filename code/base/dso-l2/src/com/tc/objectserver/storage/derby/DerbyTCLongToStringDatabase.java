/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;

import gnu.trove.TLongObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class DerbyTCLongToStringDatabase extends AbstractDerbyTCDatabase implements TCLongToStringDatabase {
  private final String loadMappingsIntoQuery;
  private final String insertQuery;

  public DerbyTCLongToStringDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    loadMappingsIntoQuery = "SELECT " + KEY + "," + VALUE + " FROM " + tableName;
    insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createLongToStringDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      // "SELECT " + KEY + "," + VALUE + " FROM " + tableName
      PreparedStatement psSelect = getOrCreatePreparedStatement(tx, loadMappingsIntoQuery);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        target.put(rs.getLong(1), rs.getString(2));
      }
      return target;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
      tx.commit();
    }
  }

  public Status insert(long id, String b, PersistenceTransaction tx) {
    try {
      // "INSERT INTO " + tableName + " VALUES (?, ?)"
      PreparedStatement psPut = getOrCreatePreparedStatement(tx, insertQuery);
      psPut.setLong(1, id);
      psPut.setString(2, b);
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
