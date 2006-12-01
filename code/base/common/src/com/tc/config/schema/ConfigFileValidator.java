/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;

/**
 * Validates a configuration file, and throws an exception if it fails.
 */
public class ConfigFileValidator {

  private String description;

  public void validate(String[] args) throws ConfigurationSetupException {
    StandardTVSConfigurationSetupManagerFactory factory;

    factory = new StandardTVSConfigurationSetupManagerFactory(args, true, new FatalIllegalConfigurationChangeHandler());
    L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
    description = manager.describeSources();
  }

  public String toString() {
    return this.description;
  }

  public static void main(String[] args) {
    ConfigFileValidator validator = new ConfigFileValidator();

    try {
      validator.validate(args);
      System.err.println(validator.toString() + ": VALID.");
      System.exit(0);
    } catch (ConfigurationSetupException cse) {
      Throwable exception = cse;

      System.err.println(validator.toString() + ": INVALID.");

      while (exception != null) {
        System.err.println(exception.getLocalizedMessage());
        exception = exception.getCause();
      }

      System.exit(1);
    }
  }

}
