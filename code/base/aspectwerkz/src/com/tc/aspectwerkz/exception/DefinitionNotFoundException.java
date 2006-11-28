/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.aspectwerkz.exception;

/**
 * Thrown when no aspectwerkz definition file or class could be found.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class DefinitionNotFoundException extends RuntimeException {
  /**
   * Sets a message.
   *
   * @param message the message
   */
  public DefinitionNotFoundException(final String message) {
    super(message);
  }
}