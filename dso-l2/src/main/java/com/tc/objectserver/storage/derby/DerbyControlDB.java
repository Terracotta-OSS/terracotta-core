/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This class is for control db
 */
class DerbyControlDB extends AbstractDerbyTCDatabase {
  private final static short                  CLEAN_FLAG = 1;
  private final static short                  DIRTY_FLAG = 2;
  private final static short                  NULL_FLAG  = -1;

  private final DerbyDBPersistenceTransaction txn;
  private final String                        insertQuery;
  private final String                        updateQuery;
  private final String                        selectQuery;

  public DerbyControlDB(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    this.txn = new DerbyDBPersistenceTransaction(connection);
    this.insertQuery = "INSERT INTO " + tableName + " (" + KEY + ", " + VALUE + ") VALUES (?, ?)";
    this.updateQuery = "UPDATE " + tableName + " SET " + VALUE + " = ? " + " WHERE " + KEY + " = ?";
    this.selectQuery = "SELECT " + VALUE + " FROM " + tableName + " WHERE " + KEY + " = ?";
  }

  @Override
  protected void createTableIfNotExists(Connection conn, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(conn, tableName)) { return; }

    String query = queryProvider.createControlDBTable(tableName, KEY, VALUE);
    executeQuery(conn, query);
  }

  public boolean isClean() throws TCDatabaseException {
    short flag = getFlag();
    return flag == CLEAN_FLAG || flag == NULL_FLAG;
  }

  public void setClean() throws TCDatabaseException {
    short flag = getFlag();
    if (flag == CLEAN_FLAG) {
      return;
    } else if (flag == NULL_FLAG) {
      insert(CLEAN_FLAG);
    } else {
      update(CLEAN_FLAG);
    }
  }

  public void setDirty() throws TCDatabaseException {
    short flag = getFlag();
    if (flag == DIRTY_FLAG) {
      return;
    } else if (flag == NULL_FLAG) {
      insert(DIRTY_FLAG);
    } else {
      update(DIRTY_FLAG);
    }
  }

  private void insert(short flag) throws TCDatabaseException {
    try {
      PreparedStatement psPut = super.getOrCreatePreparedStatement(txn, insertQuery);
      psPut.setString(1, KEY);
      psPut.setShort(2, flag);

      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
    txn.commit();
  }

  private void update(short flag) throws TCDatabaseException {
    try {
      PreparedStatement psUpdate = super.getOrCreatePreparedStatement(txn, updateQuery);
      psUpdate.setShort(1, flag);
      psUpdate.setString(2, KEY);

      psUpdate.executeUpdate();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
    txn.commit();
  }

  private short getFlag() throws TCDatabaseException {
    ResultSet rs = null;
    short shortVal = -1;
    try {
      PreparedStatement psSelect = super.getOrCreatePreparedStatement(txn, selectQuery);
      psSelect.setString(1, KEY);
      rs = psSelect.executeQuery();

      if (!rs.next()) {
        shortVal = NULL_FLAG;
      } else {
        shortVal = rs.getShort(1);
      }
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    } finally {
      closeResultSet(rs);
    }

    txn.commit();
    return shortVal;
  }
}
