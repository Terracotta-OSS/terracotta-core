/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

public class DatabaseDirtyException extends TCDatabaseException {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  public DatabaseDirtyException(Exception cause) {
    super(cause);
  }

  public DatabaseDirtyException(String message) {
    super(wrapper.wrap(message));
  }

}
