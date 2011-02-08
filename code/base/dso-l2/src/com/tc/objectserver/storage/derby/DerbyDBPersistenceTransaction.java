/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.derby.DerbyPersistenceTransactionProvider.ConnectionCommitListener;

import java.sql.Connection;
import java.sql.SQLException;

class DerbyDBPersistenceTransaction implements PersistenceTransaction {
  private final Connection               connection;
  private final ConnectionCommitListener listener;
  private boolean                        inUse = false;

  public DerbyDBPersistenceTransaction(Connection connection) {
    this(connection, null);
  }

  public DerbyDBPersistenceTransaction(Connection connection, ConnectionCommitListener commitListener) {
    this.connection = connection;
    this.listener = commitListener;
  }

  public void abort() {
    try {
      connection.rollback();
      inUse = false;
      notifyListerners();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void commit() {
    try {
      connection.commit();
      inUse = false;
      notifyListerners();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Connection getTransaction() {
    return connection;
  }

  boolean canReUse() {
    return !inUse;
  }

  void markUsed() {
    inUse = true;
  }

  private void notifyListerners() {
    if (listener != null) {
      listener.transactionCommitted(this);
    }
  }
}
