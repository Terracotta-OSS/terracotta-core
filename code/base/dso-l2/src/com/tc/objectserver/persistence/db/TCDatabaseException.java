/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.TCException;

import java.sql.SQLException;

public class TCDatabaseException extends TCException {
  public TCDatabaseException(Exception cause) {
    super(cause);
  }

  public TCDatabaseException(SQLException cause) {
    super(cause);
  }

  public TCDatabaseException(String message) {
    super(message);
  }
}
