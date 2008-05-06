/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.exception.TCRuntimeException;

/**
 * @author teck TODO: document me!
 */
public class TCBufferUnderflowException extends TCRuntimeException {

  public TCBufferUnderflowException() {
    super();
  }

  public TCBufferUnderflowException(Throwable cause) {
    super(cause);
  }

}