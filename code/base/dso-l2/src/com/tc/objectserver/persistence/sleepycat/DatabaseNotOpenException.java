/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;


public class DatabaseNotOpenException extends TCDatabaseException {

  public DatabaseNotOpenException(String message) {
    super(message);
  }

}
