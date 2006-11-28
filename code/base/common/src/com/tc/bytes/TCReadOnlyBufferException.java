package com.tc.bytes;

import com.tc.exception.TCRuntimeException;

/**
 * @author teck TODO: document me!
 */
public class TCReadOnlyBufferException extends TCRuntimeException {

  public TCReadOnlyBufferException() {
    super();
  }

  public TCReadOnlyBufferException(Throwable cause) {
    initCause(cause);
  }
}