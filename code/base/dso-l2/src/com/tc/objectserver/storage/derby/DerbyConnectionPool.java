/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.derby.DerbyPersistenceTransactionProvider.ConnectionCommitListener;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DerbyConnectionPool implements ConnectionCommitListener {
  private static final int                                 MAX_CONNECTION_TO_POOL = 50;
  private static final TCLogger                            logger                 = TCLogging
                                                                                      .getLogger(DerbyConnectionPool.class);
  private final DerbyDBEnvironment                         derbyDBEnv;
  private final List<Connection>                           pooledConnections      = new ArrayList<Connection>();

  private final ThreadLocal<DerbyDBPersistenceTransaction> threadLocalTxn         = new ThreadLocal<DerbyDBPersistenceTransaction>() {
                                                                                    @Override
                                                                                    protected DerbyDBPersistenceTransaction initialValue() {
                                                                                      return new DerbyDBPersistenceTransaction(
                                                                                                                               getOrCreateConnection());
                                                                                    }
                                                                                  };

  public DerbyConnectionPool(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
  }

  public DerbyDBPersistenceTransaction getTransaction() {
    DerbyDBPersistenceTransaction tx = threadLocalTxn.get();
    if (!tx.canReUse()) {
      tx = new DerbyDBPersistenceTransaction(getOrCreateConnection());
    }
    tx.markUsed();
    return tx;
  }

  public void transactionCommitted(DerbyDBPersistenceTransaction derbyTxn) {
    Connection connectionToPool = derbyTxn.getTransaction();
    synchronized (pooledConnections) {
      if (pooledConnections.size() <= MAX_CONNECTION_TO_POOL) {
        pooledConnections.add(connectionToPool);
        return;
      }
    }
    closeConnection(connectionToPool);
  }

  private Connection getOrCreateConnection() {
    Connection connection = null;
    synchronized (pooledConnections) {
      int size = pooledConnections.size();
      if (size > 0) {
        connection = pooledConnections.remove(size - 1);
      }
    }
    return connection == null ? createNewConnection() : connection;
  }

  private void closeConnection(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      logger.info("Error trying to close derby Connection");
    }
  }

  private Connection createNewConnection() {
    try {
      return derbyDBEnv.createConnection();
    } catch (Exception e) {
      throw new DBException(e);
    }
  }
}
