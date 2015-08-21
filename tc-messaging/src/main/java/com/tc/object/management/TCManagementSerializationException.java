/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.management;

import com.tc.exception.TCRuntimeException;

/**
 *
 */
public class TCManagementSerializationException extends TCRuntimeException {
  public TCManagementSerializationException(String msg, Throwable t) {
    super(msg, t);
  }
}
