/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;


public class DatabaseOpenException extends TCDatabaseException {
  public DatabaseOpenException(String message) {
    super(message);
  }
}
