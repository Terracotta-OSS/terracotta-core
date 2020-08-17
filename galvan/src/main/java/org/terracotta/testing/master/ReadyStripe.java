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

import org.terracotta.testing.config.ClusterInfo;
import org.terracotta.testing.config.StripeConfiguration;
import org.terracotta.testing.logging.ContextualLogger;
import org.terracotta.testing.logging.VerboseManager;

/**
 * A helper to install, configure, and start a single stripe, along with read-only data describing how to interact with it.
 */
public class ReadyStripe {
  private final IMultiProcessControl stripeControl;
  private final String stripeUri;
  private final ClusterInfo clusterInfo;

  private ReadyStripe(IMultiProcessControl stripeControl, String stripeUri, ClusterInfo clusterInfo) {
    this.stripeControl = stripeControl;
    this.stripeUri = stripeUri;
    this.clusterInfo = clusterInfo;
  }

  public IMultiProcessControl getStripeControl() {
    return stripeControl;
  }

  public String getStripeUri() {
    return stripeUri;
  }

  public ClusterInfo getClusterInfo() {
    return clusterInfo;
  }

  /**
   * Installs, configures, and starts the described stripe.
   * When this call returns, all the servers in the stripe will be installed an running (at least in an "unknownRunning"
   * state within the given interlock.
   *
   * @return The objects required to interact with and control the stripe.
   * @throws GalvanFailureException Thrown in case starting the servers in the stripe experienced a failure.
   */
  public static ReadyStripe configureAndStartStripe(GalvanStateInterlock interlock, VerboseManager stripeVerboseManager,
                                                    StripeConfiguration stripeConfig, StripeInstaller stripeInstaller) throws GalvanFailureException {
    // Create the process control object.
    ContextualLogger processControlLogger = stripeVerboseManager.createComponentManager("[ProcessControl]").createHarnessLogger();
    // Register the stripe into it and start up the server in the stripe.
    stripeInstaller.startServers();

    // Before we return, we want to wait for all the servers in the stripe to come up.
    interlock.waitForAllServerRunning();

    // Also, so we don't start the test in a racy state, wait for all the now-running servers to enter a meaningful state.
    try {
      interlock.waitForAllServerReady();
    } catch (GalvanFailureException failed) {
//  failed to start normally but some later interaction with the framework should catch this
      System.err.println("Galvan cluster failed to start:" + failed.getMessage());
    }

    // We can now create the information required by the ReadyStripe and return control to the caller to run the test or install clients.
    SynchronousProcessControl processControl = new SynchronousProcessControl(interlock, processControlLogger);
    return new ReadyStripe(processControl, stripeConfig.getUri(), stripeConfig.getClusterInfo());
  }
}
