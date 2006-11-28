/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;

public class DatabaseDirtyException extends TCDatabaseException implements com.tc.exception.DatabaseException {

  public DatabaseDirtyException(DatabaseException cause) {
    super(cause);
  }

  public DatabaseDirtyException(String message) {
    super(message);
  }

}
