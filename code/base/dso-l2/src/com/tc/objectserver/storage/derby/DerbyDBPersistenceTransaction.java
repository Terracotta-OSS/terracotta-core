/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

class DerbyDBPersistenceTransaction implements PersistenceTransaction {
  private final Connection connection;
  private final Map        properties = new HashMap(1);

  public DerbyDBPersistenceTransaction(Connection conn) {
    this.connection = conn;
  }

  public void abort() {
    try {
      connection.rollback();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void commit() {
    try {
      connection.commit();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Object getProperty(Object key) {
    return properties.get(key);
  }

  public Object setProperty(Object key, Object value) {
    return properties.put(key, value);
  }

  public Connection getTransaction() {
    return connection;
  }

}
