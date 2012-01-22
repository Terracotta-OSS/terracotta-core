/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.tools.BootJarException;

/**
 * This exception is thrown when the contents of the bootjar does not match
 * the list of pre-instrumented classes in the bootjar spec
 */
public class IncompleteBootJarException extends BootJarException {
  protected IncompleteBootJarException(String message) {
    super(message);
  }  
}
