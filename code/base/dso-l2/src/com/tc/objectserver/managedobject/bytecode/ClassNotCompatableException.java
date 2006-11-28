/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject.bytecode;

import com.tc.exception.TCRuntimeException;

public class ClassNotCompatableException extends TCRuntimeException {

  public ClassNotCompatableException() {
    super();
  }

  public ClassNotCompatableException(String message) {
    super(message);
  }

  public ClassNotCompatableException(Throwable cause) {
    super(cause);
  }

  public ClassNotCompatableException(String message, Throwable cause) {
    super(message, cause);
  }

}
