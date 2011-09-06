/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.TCDatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public class DerbyTCTransactionStoreDatabase extends DerbyTCLongToBytesDatabase {
  public DerbyTCTransactionStoreDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createTransactionStoreTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }
}
