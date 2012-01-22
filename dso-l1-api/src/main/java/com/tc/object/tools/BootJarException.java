/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tools;

public class BootJarException extends Exception {

  public BootJarException(String message) {
    super(message);
  }

  public BootJarException(String message, Throwable t) {
    super(message, t);
  }

}
