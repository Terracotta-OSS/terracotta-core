/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.tools.BootJarException;

/**
 * 
 */
public class UnverifiedBootJarException extends BootJarException {

  protected UnverifiedBootJarException(String message) {
    super(message);
  }

  protected UnverifiedBootJarException(String message, Throwable t) {
    super(message, t);
  }
}
