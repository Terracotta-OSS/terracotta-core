/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.TCRuntimeException;

/**
 * Terracotta and Data store have to be of same version.
 */
public class DBVersionMismatchException extends TCRuntimeException {
  public DBVersionMismatchException(String message) {
    super(message);
  }
}
