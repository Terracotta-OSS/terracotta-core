/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * An {@link com.tc.config.schema.IllegalConfigurationChangeHandler} that prints
 * a message to the screen and the logs, and then exits.
 *
 * NOTE: this code should no longer be engaged since the configuration modes
 * were slimmed down to production and development. In production the client
 * config is checked to match that of the servers. In development, the
 * client's config simply applies to that client, whereas it used to be
 * broadcast to other clients. Basically, config values cannot change after
 * startup anymore.
 */
public class FatalIllegalConfigurationChangeHandler implements IllegalConfigurationChangeHandler {

  private static TCLogger logger;

  private TCLogger getLogger() {
    if(logger == null) {
      logger = TCLogging.getLogger(FatalIllegalConfigurationChangeHandler.class);      
    }
    
    return logger;
  }
  
  public void changeFailed(ConfigItem item, Object oldValue, Object newValue) {
    String text = "Error: Terracotta is using an inconsistent configuration.\n\n"
                  + "The configuration that this client is using is different from the one used by\n"
                  + "the connected production server.\n\n" + "Specific information: " + item + " has changed.\n"
                  + "   Old value: " + describe(oldValue) + "\n" + "   New value: " + describe(newValue) + "\n";

    System.err.println(text);
    getLogger().fatal(text);
    System.exit(3);
  }

  private String describe(Object o) {
    if (o == null) return "<null>";
    if (o.getClass().isArray()) return ArrayUtils.toString(o);
    else return o.toString();
  }

}
