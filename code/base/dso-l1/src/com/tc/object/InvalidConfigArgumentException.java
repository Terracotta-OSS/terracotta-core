/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCRuntimeException;

public class InvalidConfigArgumentException extends TCRuntimeException {
  /**
   * @param message
   */
  public InvalidConfigArgumentException(String message) {
    super(message);
  }

}