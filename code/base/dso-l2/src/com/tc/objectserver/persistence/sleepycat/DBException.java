/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

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
