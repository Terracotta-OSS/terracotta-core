/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.TCDatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public class DerbyTCBytesToBytesDatabase extends DerbyTCBytesToBlobDatabase {

  public DerbyTCBytesToBytesDatabase(String tableName, Connection connection, QueryProvider queryProvider)
      throws TCDatabaseException {
    super(tableName, connection, queryProvider);
  }

  @Override
  protected final void createTableIfNotExists(Connection connection, QueryProvider queryProvider) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    String query = queryProvider.createBytesToBytesDBTable(tableName, KEY, VALUE);
    executeQuery(connection, query);
  }
}
