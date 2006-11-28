/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;

public class TCDatabaseException extends DatabaseException {
  public TCDatabaseException(DatabaseException cause) {
    super(cause);
  }
  
  public TCDatabaseException(String message) {
    super(message);
  }
}
