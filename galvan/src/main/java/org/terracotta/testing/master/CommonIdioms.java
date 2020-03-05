/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.master;

import org.terracotta.testing.config.ClientsConfiguration;
import org.terracotta.testing.config.StripeConfiguration;
import org.terracotta.testing.logging.VerboseManager;

import java.io.IOException;

/**
 * This class is meant to be a container of helpers and testing idioms which are common to different entry-points into the framework.
 * It exists purely to avoid duplication.
 */
public class CommonIdioms {
  public static ReadyStripe setupConfigureAndStartStripe(GalvanStateInterlock interlock, ITestStateManager stateManager,
                                                         VerboseManager verboseManager, StripeConfiguration stripeConfig)
      throws IOException, GalvanFailureException {
    VerboseManager stripeVerboseManager = verboseManager.createComponentManager("[" + stripeConfig.getStripeName() + "]");
    return ReadyStripe.configureAndStartStripe(interlock, stateManager, stripeVerboseManager, stripeConfig);
  }

  /**
   * Note that the clients will be run in another thread, logging to the given logger and returning their state in stateManager.
   */
  public static void installAndRunClients(IGalvanStateInterlock interlock, ITestStateManager stateManager, VerboseManager verboseManager,
                                          ClientsConfiguration clientsConfiguration, IMultiProcessControl processControl) {
    ClientSubProcessManager manager = new ClientSubProcessManager(interlock, stateManager, verboseManager, processControl, clientsConfiguration);
    manager.start();
  }
}
