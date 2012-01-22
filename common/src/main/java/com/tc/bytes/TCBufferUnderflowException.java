/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
