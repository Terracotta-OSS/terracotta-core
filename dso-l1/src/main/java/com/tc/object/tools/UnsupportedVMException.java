/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

public class UnsupportedVMException extends BootJarException {

  public UnsupportedVMException(final String message) {
    super(message);
  }

  public UnsupportedVMException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
