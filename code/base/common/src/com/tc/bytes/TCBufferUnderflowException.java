/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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