/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.TCDatabaseCursor;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDerbyTCDatabaseCursor<K, V> implements TCDatabaseCursor<K, V> {
  private static final TCLogger logger   = TCLogging.getLogger(AbstractDerbyTCDatabaseCursor.class);

  protected final ResultSet     rs;
  protected volatile boolean    isClosed = false;

  public AbstractDerbyTCDatabaseCursor(ResultSet rs) {
    this.rs = rs;
  }

  public void close() {
    try {
      rs.close();
      isClosed = true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void delete() {
    try {
      rs.deleteRow();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (!isClosed) {
      logger.info("Since the closed for the cursor was not called. So calling it explicity in finalize.");
      close();
    }
    super.finalize();
  }
}
