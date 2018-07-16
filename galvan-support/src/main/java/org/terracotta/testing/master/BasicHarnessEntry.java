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

import java.io.IOException;

import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.logging.VerboseManager;


/**
 * The harness entry-point for the harness running {@link BasicTestClusterConfiguration} tests.
 */
public class BasicHarnessEntry extends AbstractHarnessEntry<BasicTestClusterConfiguration> {
  // Run the one configuration.
  @Override
  protected void runOneConfiguration(VerboseManager verboseManager, DebugOptions debugOptions, CommonHarnessOptions harnessOptions, BasicTestClusterConfiguration runConfiguration) throws IOException, GalvanFailureException {
    int serversToCreate = runConfiguration.serversInStripe;
    Assert.assertTrue(serversToCreate > 0);
    
    // Calculate the size of the port range we need:  each server needs 2 ports.
    int portsRequired = serversToCreate * 2;
    
    CommonIdioms.StripeConfiguration stripeConfiguration = new CommonIdioms.StripeConfiguration();
    stripeConfiguration.kitOriginPath = harnessOptions.kitOriginPath;
    stripeConfiguration.testParentDirectory = harnessOptions.configTestDirectory;
    stripeConfiguration.serversToCreate = serversToCreate;
    stripeConfiguration.serverHeapInM = harnessOptions.serverHeapInM;
    stripeConfiguration.serverStartPort = chooseRandomPortRange(portsRequired);
    stripeConfiguration.serverDebugPortStart = debugOptions.serverDebugPortStart;
    stripeConfiguration.serverStartNumber = 0;
    stripeConfiguration.extraJarPaths = harnessOptions.extraJarPaths;
    stripeConfiguration.namespaceFragment = harnessOptions.namespaceFragment;
    stripeConfiguration.serviceFragment = harnessOptions.serviceFragment;
    stripeConfiguration.clientReconnectWindowTime = harnessOptions.clientReconnectWindowTime;
    stripeConfiguration.tcProperties = harnessOptions.tcProperties;
    stripeConfiguration.logConfigExtension = "logback-ext.xml";
    // This is the simple case of a single-stripe so we don't need to wrap or decode anything.
    stripeConfiguration.stripeName = "stripe" + 0;
    
    TestStateManager stateManager = new TestStateManager();
    GalvanStateInterlock interlock = new GalvanStateInterlock(verboseManager.createComponentManager("[Interlock]").createHarnessLogger(), stateManager);
    ReadyStripe oneStripe = CommonIdioms.setupConfigureAndStartStripe(interlock, stateManager, verboseManager, stripeConfiguration);
    // We just want to unwrap this, directly.
    IMultiProcessControl processControl = oneStripe.stripeControl;
    String connectUri = oneStripe.stripeUri;
    ClusterInfo clusterInfo = oneStripe.clusterInfo;
    Assert.assertTrue(null != processControl);
    Assert.assertTrue(null != connectUri);
    
    // The cluster is now running so install and run the clients.
    CommonIdioms.ClientsConfiguration clientsConfiguration = new CommonIdioms.ClientsConfiguration();
    clientsConfiguration.testParentDirectory = harnessOptions.configTestDirectory;
    clientsConfiguration.clientClassPath = harnessOptions.clientClassPath;
    clientsConfiguration.clientsToCreate = harnessOptions.clientsToCreate;
    clientsConfiguration.clientArgumentBuilder = new BasicClientArgumentBuilder(harnessOptions.testClassName, harnessOptions.errorClassName);
    clientsConfiguration.connectUri = connectUri;
    clientsConfiguration.clusterInfo = clusterInfo;
    // The basic harness can only run single-stripe tests.
    clientsConfiguration.numberOfStripes = 1;
    clientsConfiguration.numberOfServersPerStripe = runConfiguration.serversInStripe;
    clientsConfiguration.setupClientDebugPort = debugOptions.setupClientDebugPort;
    clientsConfiguration.destroyClientDebugPort = debugOptions.destroyClientDebugPort;
    clientsConfiguration.testClientDebugPortStart = debugOptions.testClientDebugPortStart;
    clientsConfiguration.failOnLog = harnessOptions.failOnLog;
    CommonIdioms.installAndRunClients(interlock, stateManager, verboseManager, clientsConfiguration, processControl);
    // NOTE:  waitForFinish() throws GalvanFailureException on failure.
    try {
      stateManager.waitForFinish();
    } finally {
      // No matter what happened, shut down the test.
      interlock.forceShutdown();
    }
  }
}
