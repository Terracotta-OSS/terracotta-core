/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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