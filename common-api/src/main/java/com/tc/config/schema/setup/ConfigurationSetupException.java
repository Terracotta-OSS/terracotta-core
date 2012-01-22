/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCException;

/**
 * Thrown when the configuration system couldn't be set up. This should generally be treated as a fatal exception.
 */
public class ConfigurationSetupException extends TCException {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  public ConfigurationSetupException() {
    super();
  }

  public ConfigurationSetupException(String message) {
    super(wrapper.wrap(message));
  }

  public ConfigurationSetupException(Throwable cause) {
    super(cause);
  }

  public ConfigurationSetupException(String message, Throwable cause) {
    super(wrapper.wrap(message), cause);
  }

}
