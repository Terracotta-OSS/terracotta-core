/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.exception.TCRuntimeException;

public class DBException extends TCRuntimeException {

  public DBException(String message, Throwable t) {
    super(message, t);
  }
  
  public DBException(Throwable t) {
    //super((t instanceof RuntimeExceptionWrapper) ? ((RuntimeExceptionWrapper)t).getDetail() : t);
    super(t);
  }
  
  public DBException(String string) {
    super(string);
  }

}
