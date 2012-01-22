/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.exception;

/**
 * Thrown when no aspectwerkz definition file or class could be found.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
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
