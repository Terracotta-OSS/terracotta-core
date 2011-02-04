/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class is for control db
 */
class DerbyControlDB extends AbstractDerbyTCDatabase {
  private final static short CLEAN_FLAG = 1;
  private final static short DIRTY_FLAG = 2;
  private final static short NULL_FLAG  = -1;

  private final Connection   connection;

  public DerbyControlDB(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
    this.connection = connection;
  }

  @Override
  protected void createTableIfNotExists(Connection conn, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(conn, tableName)) { return; }

    Statement statement = conn.createStatement();
    String query = queryProvider.createControlDBTable(tableName, KEY, VALUE);
    statement.execute(query);
    statement.close();
    conn.commit();
  }

  public boolean isClean() throws TCDatabaseException {
    int flag;
    try {
      flag = getFlag();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
    return flag == CLEAN_FLAG || flag == NULL_FLAG;
  }

  public void setClean() throws TCDatabaseException {
    short flag;
    try {
      flag = getFlag();
      if (flag == CLEAN_FLAG) {
        return;
      } else if (flag == NULL_FLAG) {
        insert(CLEAN_FLAG);
      } else {
        update(CLEAN_FLAG);
      }
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  public void setDirty() throws TCDatabaseException {
    try {
      short flag = getFlag();
      if (flag == DIRTY_FLAG) {
        return;
      } else if (flag == NULL_FLAG) {
        insert(DIRTY_FLAG);
      } else {
        update(DIRTY_FLAG);
      }
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private void insert(short flag) throws SQLException {
    PreparedStatement psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
    psPut.setString(1, KEY);
    psPut.setShort(2, flag);

    psPut.executeUpdate();
    connection.commit();
  }

  private void update(short flag) throws SQLException {
    PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                             + " WHERE " + KEY + " = ?");
    psUpdate.setShort(1, flag);
    psUpdate.setString(2, KEY);
    psUpdate.executeUpdate();

    connection.commit();
  }

  private short getFlag() throws SQLException {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setString(1, KEY);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return NULL_FLAG; }
      short shortVal = rs.getShort(1);
      return shortVal;
    } finally {
      rs.close();
      connection.commit();
    }
  }
}
