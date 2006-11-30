/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.bytes;

import com.tc.exception.TCRuntimeException;

/**
 * @author teck TODO: document me!
 */
public class TCBufferOverflowException extends TCRuntimeException {

  public TCBufferOverflowException() {
    super();
  }

  public TCBufferOverflowException(Throwable cause) {
    super(cause);
  }

}