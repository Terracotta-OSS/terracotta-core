/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.config;

/**
 * Exception type thrown when an invalid configuration is encountered.
 */
public class InvalidConfigurationException extends RuntimeException {

  public InvalidConfigurationException() {
    super();
  }

  public InvalidConfigurationException(String message) {
    super(message);
  }

  public InvalidConfigurationException(Throwable cause) {
    super(cause);
  }

  public InvalidConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
