/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  
  @Override
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
