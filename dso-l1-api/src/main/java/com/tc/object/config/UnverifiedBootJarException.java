/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
