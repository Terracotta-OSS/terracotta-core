/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCException;

/**
 * An exception thrown when an operation times out. Feel free to subclass
 * 
 * @author teck
 */
public class TCTimeoutException extends TCException {

  public TCTimeoutException(long timeout) {
    this("Timeout of " + timeout + " occurred");
  }

  public TCTimeoutException(String string) {
    super(string);
  }

  public TCTimeoutException(Throwable cause) {
    super(cause);
  }
  
  public TCTimeoutException(String reason, Throwable cause) {
    super(reason, cause);
  }
}